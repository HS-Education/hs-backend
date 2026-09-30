"""Storage and messaging adapters. Azure SDKs are imported only in Azure mode."""
import hashlib
import json
import logging
import threading
from typing import Callable
from safety import MAX_PDF_BYTES

logger = logging.getLogger(__name__)
MAX_RESULT_BYTES = 64 * 1024 * 1024


def validate_job(message: dict) -> tuple[int, int, str]:
    if (not isinstance(message, dict) or type(message.get("schemaVersion")) is not int or message.get("schemaVersion") != 1
            or type(message.get("documentId")) is not int or message["documentId"] < 1
            or type(message.get("generation")) is not int or message["generation"] < 1):
        raise ValueError("Invalid document job")
    object_key = message.get("objectKey")
    if (not isinstance(object_key, str) or not object_key.startswith("documents/")
            or any(part in object_key for part in ("..", "\\", "?", "#")) or len(object_key) > 1024):
        raise ValueError("Invalid document object key")
    return message["documentId"], message["generation"], object_key


def result_reference(payload: dict) -> tuple[dict, bytes]:
    document_id, generation = payload["documentId"], payload["generation"]
    if type(document_id) is not int or document_id < 1 or type(generation) is not int or generation < 1:
        raise ValueError("Invalid processing result")
    chunks = payload.get("chunks")
    if not isinstance(chunks, list) or not 0 < len(chunks) <= 1500:
        raise ValueError("Invalid processing result")
    body = json.dumps(payload, ensure_ascii=False, allow_nan=False, separators=(",", ":")).encode("utf-8")
    if len(body) > MAX_RESULT_BYTES:
        raise ValueError("Processing result exceeds limit")
    digest = hashlib.sha256(body).hexdigest()
    return {
        "schemaVersion": 1, "documentId": document_id, "generation": generation,
        "objectKey": f"processing-results/{document_id}/{generation}/{digest}.json",
        "sha256": digest, "sizeBytes": len(body), "chunkCount": len(chunks), "dimensions": 1024,
    }, body


class MinioDocumentStorage:
    def __init__(self, client, bucket):
        self.client, self.bucket = client, bucket

    def read_document(self, object_key):
        response = self.client.get_object(bucket_name=self.bucket, object_name=object_key)
        try:
            return response.read(MAX_PDF_BYTES + 1)
        finally:
            response.close()
            response.release_conn()


class AzureDocumentStorage:
    def __init__(self, settings):
        from azure.identity import ManagedIdentityCredential
        from azure.storage.blob import BlobServiceClient
        self.client = BlobServiceClient(settings.azure_storage_account_url, credential=ManagedIdentityCredential())
        self.documents = settings.azure_documents_container
        self.results = settings.azure_results_container

    def read_document(self, object_key):
        blob = self.client.get_blob_client(container=self.documents, blob=object_key)
        if not 0 < blob.get_blob_properties().size <= MAX_PDF_BYTES:
            raise ValueError("Invalid PDF size")
        # Bound the request itself, not only an in-memory check after a full download.
        return blob.download_blob(offset=0, length=min(blob.get_blob_properties().size, MAX_PDF_BYTES + 1),
                                  max_concurrency=1).readall()

    def store_result(self, payload):
        from azure.core.exceptions import ResourceExistsError
        from azure.storage.blob import ContentSettings
        reference, body = result_reference(payload)
        blob = self.client.get_blob_client(container=self.results, blob=reference["objectKey"])
        try:
            blob.upload_blob(body, overwrite=False, max_concurrency=1,
                             content_settings=ContentSettings(content_type="application/json"))
        except ResourceExistsError:
            # A retry writes the same content-addressed object, never a mutable shared URL.
            pass
        return reference


class AzureDocumentConsumer:
    def __init__(self, settings, storage, process: Callable[[dict], dict]):
        self.settings, self.storage, self.process = settings, storage, process
        self.stop_event = threading.Event()
        self.connected = threading.Event()

    def handle(self, message, receiver, sender, failure_sender):
        """Settle only after the result or terminal failure has been durably published."""
        try:
            body = b"".join(message.body)
            if len(body) > 16 * 1024:
                raise ValueError("Job exceeds reference limit")
            job = json.loads(body)
            document_id, generation, _ = validate_job(job)
        except (ValueError, TypeError, KeyError):
            receiver.dead_letter_message(message, reason="INVALID_CONTRACT")
            return
        try:
            payload = self.process(job)
            reference = self.storage.store_result(payload)
            sender.send_messages(self._message(reference, f"result:{document_id}:{generation}"))
            receiver.complete_message(message)
        except Exception as error:
            logger.warning("Azure document processing failed; documentId=%s errorType=%s", document_id, type(error).__name__)
            if not isinstance(error, ValueError) and message.delivery_count < self.settings.azure_max_delivery_count:
                receiver.abandon_message(message)
                return
            failure = {"schemaVersion": 1, "documentId": document_id, "generation": generation,
                       "errorCode": "DOCUMENT_INVALID" if isinstance(error, ValueError) else "PROCESSING_UNAVAILABLE"}
            # If publication fails, do not complete; the lock expires and the job is redelivered.
            failure_sender.send_messages(self._message(failure, f"failure:{document_id}:{generation}"))
            receiver.complete_message(message)

    @staticmethod
    def _message(payload, message_id):
        from azure.servicebus import ServiceBusMessage
        return ServiceBusMessage(json.dumps(payload, separators=(",", ":")), message_id=message_id,
                                 correlation_id=str(payload["documentId"]), content_type="application/json")

    def run(self):
        from azure.identity import ManagedIdentityCredential
        from azure.servicebus import AutoLockRenewer, ServiceBusClient
        while not self.stop_event.is_set():
            try:
                with ManagedIdentityCredential() as credential, ServiceBusClient(
                        self.settings.azure_servicebus_namespace, credential=credential) as client:
                    with client.get_queue_receiver(queue_name=self.settings.azure_processing_queue,
                            prefetch_count=0, max_wait_time=5) as receiver, \
                            client.get_queue_sender(queue_name=self.settings.azure_results_queue) as sender, \
                            client.get_queue_sender(queue_name=self.settings.azure_failures_queue) as failure_sender, \
                            AutoLockRenewer(max_lock_renewal_duration=self.settings.azure_lock_renewal_seconds,
                                            max_workers=1) as renewer:
                        while not self.stop_event.is_set():
                            messages = receiver.receive_messages(max_message_count=1, max_wait_time=5)
                            self.connected.set()
                            for message in messages:
                                renewer.register(receiver, message)
                                self.handle(message, receiver, sender, failure_sender)
            except Exception as error:
                self.connected.clear()
                logger.warning("Azure consumer reconnecting; errorType=%s", type(error).__name__)
                self.stop_event.wait(5)

    def stop(self):
        self.stop_event.set()
