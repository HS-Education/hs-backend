import hashlib
import json
import unittest
from types import SimpleNamespace
from unittest.mock import Mock, patch
from pydantic import ValidationError
from infrastructure import AzureDocumentConsumer, MinioDocumentStorage, result_reference, validate_job
from worker_settings import Settings, load_settings


class EnvironmentTests(unittest.TestCase):
    def azure(self, **overrides):
        return Settings(_env_file=None, app_env="azure", worker_api_key="private-worker",
                        openrouter_api_key="private-provider",
                        azure_storage_account_url="https://thesis.blob.core.windows.net/",
                        azure_servicebus_namespace="thesis.servicebus.windows.net", **overrides)

    def test_azure_does_not_require_local_credentials(self):
        settings = self.azure()
        self.assertEqual(settings.minio_endpoint, "")
        self.assertNotIn("private-worker", repr(settings))
        self.assertNotIn("private-provider", repr(settings))

    def test_local_requires_local_infrastructure(self):
        with patch.dict("os.environ", {}, clear=True), self.assertRaises(ValidationError):
            Settings(_env_file=None, worker_api_key="w", openrouter_api_key="p")

    def test_azure_never_loads_dotenv(self):
        with patch.dict("os.environ", {"APP_ENV": "azure"}), patch("worker_settings.Settings") as constructor:
            load_settings()
            constructor.assert_called_once_with(_env_file=None)

    def test_unresolved_key_vault_reference_prevents_startup(self):
        with self.assertRaises(ValidationError):
            Settings(_env_file=None, app_env="azure", worker_api_key="private-worker",
                     openrouter_api_key="@Microsoft.KeyVault(SecretUri=https://example/secrets/provider)",
                     azure_storage_account_url="https://thesis.blob.core.windows.net/",
                     azure_servicebus_namespace="thesis.servicebus.windows.net")

    def test_rejects_unbounded_embedding_dimensions_and_concurrency(self):
        for values in ({"embedding_dimensions": 768}, {"provider_max_concurrency": 100}):
            with self.subTest(values=values), self.assertRaises(ValidationError):
                self.azure(**values)

    def test_rejects_non_azure_storage_and_credentials_in_url(self):
        for url in ("http://thesis.blob.core.windows.net", "https://evil.invalid",
                    "https://user:secret@thesis.blob.core.windows.net", "https://thesis.blob.core.windows.net/?sig=key"):
            with self.subTest(url=url), self.assertRaises(ValidationError):
                Settings(_env_file=None, app_env="azure", worker_api_key="w", openrouter_api_key="p",
                         azure_storage_account_url=url, azure_servicebus_namespace="thesis.servicebus.windows.net")


class ContractTests(unittest.TestCase):
    def test_job_has_explicit_generation(self):
        self.assertEqual(validate_job({"schemaVersion": 1, "documentId": 2, "generation": 3,
                                       "objectKey": "documents/a.pdf"}), (2, 3, "documents/a.pdf"))

    def test_invalid_jobs_are_rejected(self):
        for values in ({"documentId": True}, {"generation": 0}, {"schemaVersion": 2},
                       {"objectKey": "documents/../secret"}, {"objectKey": "https://evil.invalid"},
                       {"objectKey": "documents/a?sig=key"}):
            with self.subTest(values=values), self.assertRaises(ValueError):
                validate_job({"schemaVersion": 1, "documentId": 2, "generation": 1,
                              "objectKey": "documents/a.pdf", **values})

    def test_queue_contains_reference_not_large_vectors(self):
        payload = {"documentId": 2, "generation": 1, "chunks": [
            {"pageNumber": 1, "chunkIndex": index, "content": "lesson", "embedding": [0.1] * 1024}
            for index in range(100)]}
        reference, body = result_reference(payload)
        self.assertGreater(len(body), 256 * 1024)
        self.assertLess(len(json.dumps(reference)), 1024)
        self.assertEqual(reference["sha256"], hashlib.sha256(body).hexdigest())
        self.assertEqual(reference["sizeBytes"], len(body))
        self.assertIn(reference["sha256"], reference["objectKey"])

    def test_invalid_result_size_and_nonfinite_numbers_are_rejected(self):
        for chunks in ([], [dict(embedding=[float("nan")])], [{}] * 1501):
            with self.subTest(size=len(chunks)), self.assertRaises(ValueError):
                result_reference({"documentId": 1, "generation": 1, "chunks": chunks})

    def test_local_minio_response_is_always_released(self):
        client = Mock()
        client.get_object.return_value.read.side_effect = IOError("download failed")
        with self.assertRaises(IOError):
            MinioDocumentStorage(client, "bucket").read_document("documents/a.pdf")
        client.get_object.return_value.close.assert_called_once()
        client.get_object.return_value.release_conn.assert_called_once()


class SettlementTests(unittest.TestCase):
    def setUp(self):
        self.job = {"schemaVersion": 1, "documentId": 7, "generation": 2, "objectKey": "documents/a.pdf"}
        self.message = SimpleNamespace(body=[json.dumps(self.job).encode()], delivery_count=1)
        self.receiver, self.sender, self.failures, self.storage, self.process = [Mock() for _ in range(5)]
        self.consumer = AzureDocumentConsumer(SimpleNamespace(azure_max_delivery_count=5), self.storage, self.process)
        self.consumer._message = lambda payload, identifier: (payload, identifier)

    def handle(self):
        self.consumer.handle(self.message, self.receiver, self.sender, self.failures)

    def test_blob_and_message_publication_precede_acknowledgement(self):
        sequence = Mock()
        for name, instance in (("process", self.process), ("storage", self.storage),
                               ("sender", self.sender), ("receiver", self.receiver)):
            sequence.attach_mock(instance, name)
        self.handle()
        names = [call[0] for call in sequence.mock_calls]
        self.assertLess(names.index("storage.store_result"), names.index("sender.send_messages"))
        self.assertLess(names.index("sender.send_messages"), names.index("receiver.complete_message"))
        self.failures.send_messages.assert_not_called()

    def test_transient_error_abandons_without_ack(self):
        self.process.side_effect = IOError("provider unavailable")
        self.handle()
        self.receiver.abandon_message.assert_called_once_with(self.message)
        self.receiver.complete_message.assert_not_called()
        self.failures.send_messages.assert_not_called()

    def test_terminal_error_publishes_safe_failure_then_ack(self):
        self.process.side_effect = ValueError("private corrupt file information")
        self.handle()
        payload, identifier = self.failures.send_messages.call_args.args[0]
        self.assertEqual(payload["errorCode"], "DOCUMENT_INVALID")
        self.assertEqual(payload["generation"], 2)
        self.assertNotIn("private", json.dumps(payload))
        self.assertEqual(identifier, "failure:7:2")
        self.receiver.complete_message.assert_called_once()

    def test_retry_exhaustion_reports_failure(self):
        self.message.delivery_count = 5
        self.process.side_effect = IOError("provider unavailable")
        self.handle()
        self.assertEqual(self.failures.send_messages.call_args.args[0][0]["errorCode"], "PROCESSING_UNAVAILABLE")
        self.receiver.complete_message.assert_called_once()

    def test_failure_publication_failure_does_not_ack(self):
        self.process.side_effect = ValueError("corrupt")
        self.failures.send_messages.side_effect = IOError("bus offline")
        with self.assertRaises(IOError):
            self.handle()
        self.receiver.complete_message.assert_not_called()

    def test_bad_job_goes_to_dead_letter_without_processing(self):
        self.message.body = [b'{"documentId":true}']
        self.handle()
        self.receiver.dead_letter_message.assert_called_once()
        self.process.assert_not_called()

    def test_result_publication_failure_retries_durable_result(self):
        self.sender.send_messages.side_effect = IOError("bus offline")
        self.handle()
        self.storage.store_result.assert_called_once()
        self.receiver.abandon_message.assert_called_once()
        self.receiver.complete_message.assert_not_called()


if __name__ == "__main__":
    unittest.main()
