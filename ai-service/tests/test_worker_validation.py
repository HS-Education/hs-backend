import importlib
import os
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from fastapi import HTTPException


FAKE_ENV = {
    "WORKER_API_KEY": "offline-worker-key",
    "MINIO_ENDPOINT": "localhost:9000",
    "MINIO_ROOT_USER": "offline-user",
    "MINIO_ROOT_PASSWORD": "offline-password",
    "MINIO_BUCKET_NAME": "offline-bucket",
    "RABBITMQ_HOST": "localhost",
    "RABBITMQ_USER": "offline-user",
    "RABBITMQ_PASSWORD": "offline-password",
    "RABBITMQ_PORT": "5672",
    "OPENROUTER_API_KEY": "offline-openrouter-key",
}


def fake_client(content):
    completion = SimpleNamespace(choices=[SimpleNamespace(message=SimpleNamespace(content=content))])
    create = lambda **_kwargs: completion
    return SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(create=create)))


class WorkerValidationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with patch.dict(os.environ, FAKE_ENV):
            cls.worker = importlib.import_module("main")

    def test_quiz_accepts_valid_structured_response(self):
        content = ('{"questions":[{"text":"2+2?","options":["1","2","3","4"],'
                   '"correctOptionIndex":3,"isRemedial":false,"difficulty":"LOW"}]}')
        with patch.object(self.worker, "openai_client", fake_client(content)):
            response = self.worker.generate_quiz(self.worker.GenerateQuizRequest(
                context_text="synthetic", topic_name="math", num_questions=1, is_remedial=False))
        self.assertEqual(response.questions[0].correctOptionIndex, 3)

    def test_quiz_rejects_invalid_answer_index_without_echoing_content(self):
        content = ('{"questions":[{"text":"private-canary","options":["1","2","3","4"],'
                   '"correctOptionIndex":9,"isRemedial":false,"difficulty":"LOW"}]}')
        with patch.object(self.worker, "openai_client", fake_client(content)):
            with self.assertRaises(HTTPException) as caught:
                self.worker.generate_quiz(self.worker.GenerateQuizRequest(
                    context_text="synthetic", topic_name="math", num_questions=1, is_remedial=False))
        self.assertEqual(caught.exception.status_code, 500)
        self.assertNotIn("private-canary", caught.exception.detail)


if __name__ == "__main__":
    unittest.main()
