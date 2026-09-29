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


def fake_client(content, captured=None, finish_reason=None):
    completion = SimpleNamespace(choices=[SimpleNamespace(
        message=SimpleNamespace(content=content), finish_reason=finish_reason)])

    def create(**kwargs):
        if captured is not None:
            captured.update(kwargs)
        return completion

    return SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(create=create)))


class WorkerValidationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with patch.dict(os.environ, FAKE_ENV):
            cls.worker = importlib.import_module("main")

    def test_quiz_accepts_valid_structured_response(self):
        captured = {}
        content = ('{"questions":[{"text":"2+2?","options":["1","2","3","4"],'
                   '"correctOptionIndex":3,"isRemedial":false,"difficulty":"LOW"}]}')
        with patch.object(self.worker, "openai_client", fake_client(content, captured)):
            response = self.worker.generate_quiz(self.worker.GenerateQuizRequest(
                context_text="synthetic", topic_name="math", num_questions=1, is_remedial=False))
        self.assertEqual(response.questions[0].correctOptionIndex, 3)
        self.assertGreaterEqual(captured["max_tokens"], 1024)
        self.assertEqual(captured["extra_body"]["reasoning"]["effort"], "low")

    def test_quiz_rejects_invalid_answer_index_without_echoing_content(self):
        content = ('{"questions":[{"text":"private-canary","options":["1","2","3","4"],'
                   '"correctOptionIndex":9,"isRemedial":false,"difficulty":"LOW"}]}')
        with patch.object(self.worker, "openai_client", fake_client(content)):
            with self.assertRaises(HTTPException) as caught:
                self.worker.generate_quiz(self.worker.GenerateQuizRequest(
                    context_text="synthetic", topic_name="math", num_questions=1, is_remedial=False))
        self.assertEqual(caught.exception.status_code, 500)
        self.assertNotIn("private-canary", caught.exception.detail)

    def test_quiz_rejects_wrong_question_count_without_echoing_content(self):
        content = ('{"questions":[{"text":"private-canary","options":["1","2","3","4"],'
                   '"correctOptionIndex":3,"isRemedial":false,"difficulty":"LOW"},'
                   '{"text":"extra","options":["1","2","3","4"],'
                   '"correctOptionIndex":3,"isRemedial":false,"difficulty":"LOW"}]}')
        with patch.object(self.worker, "openai_client", fake_client(content)):
            with self.assertRaises(HTTPException) as caught:
                self.worker.generate_quiz(self.worker.GenerateQuizRequest(
                    context_text="synthetic", topic_name="math", num_questions=1, is_remedial=False))
        self.assertEqual(caught.exception.status_code, 500)
        self.assertNotIn("private-canary", caught.exception.detail)

    def test_structured_routes_reject_provider_token_limit_without_echoing_partial_json(self):
        quiz_content = '{"questions":[{"text":"partial private-canary"'
        quiz_client = fake_client(quiz_content, finish_reason="length")
        with patch.object(self.worker, "openai_client", quiz_client):
            with self.assertRaises(HTTPException) as quiz_error:
                self.worker.generate_quiz(self.worker.GenerateQuizRequest(
                    context_text="synthetic", topic_name="math", num_questions=1, is_remedial=False))
        self.assertEqual(quiz_error.exception.status_code, 500)
        self.assertNotIn("private-canary", quiz_error.exception.detail)

        feedback_client = fake_client('{"feedbacks":[{"feedback":"partial private-canary"',
                                      finish_reason="length")
        payload = self.worker.GenerateFeedbackRequest.model_validate(self.feedback_request((11,)))
        with patch.object(self.worker, "openai_client", feedback_client):
            with self.assertRaises(HTTPException) as feedback_error:
                self.worker.generate_feedback(payload)
        self.assertEqual(feedback_error.exception.status_code, 500)
        self.assertNotIn("private-canary", feedback_error.exception.detail)

    @staticmethod
    def feedback_request(ids=(11, 12)):
        return {
            "wrong_answers": [
                {
                    "question_id": question_id,
                    "topic": "aritmética",
                    "question_text": "¿Cuánto es 2 + 2?",
                    "student_answer": "3",
                    "correct_answer": "4",
                }
                for question_id in ids
            ]
        }

    def test_feedback_requires_exact_question_ids_and_returns_request_order(self):
        captured = {}
        content = ('{"feedbacks":[{"questionId":12,"feedback":"Primero revisa la suma."},'
                   '{"questionId":11,"feedback":"Dos más dos es cuatro."}]}')
        payload = self.worker.GenerateFeedbackRequest.model_validate(self.feedback_request())
        with patch.object(self.worker, "openai_client", fake_client(content, captured)):
            response = self.worker.generate_feedback(payload)
        self.assertEqual([item.questionId for item in response.feedbacks], [11, 12])
        self.assertGreaterEqual(captured["max_tokens"], 1024)
        self.assertEqual(captured["extra_body"]["reasoning"]["effort"], "low")

    def test_truncated_text_answer_is_rejected_without_returning_partial_content(self):
        completion = SimpleNamespace(choices=[SimpleNamespace(
            message=SimpleNamespace(content="partial secret-canary"), finish_reason="length")])
        client = SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(
            create=lambda **_kwargs: completion)))
        with patch.object(self.worker, "openai_client", client):
            with self.assertRaises(HTTPException) as caught:
                self.worker.generate_answer(self.worker.GenerateRequest(messages=[
                    self.worker.Message(role="user", content="Explain 2 + 2")]))
        self.assertEqual(caught.exception.status_code, 500)
        self.assertNotIn("partial secret-canary", caught.exception.detail)

    def test_feedback_rejects_missing_extra_or_duplicate_model_ids_without_echoing(self):
        for content in (
            '{"feedbacks":[{"questionId":11,"feedback":"private-canary"}]}',
            ('{"feedbacks":[{"questionId":11,"feedback":"private-canary"},'
             '{"questionId":12,"feedback":"ok"},{"questionId":99,"feedback":"extra"}]}'),
            ('{"feedbacks":[{"questionId":11,"feedback":"private-canary"},'
             '{"questionId":11,"feedback":"duplicate"}]}'),
        ):
            with self.subTest(content=content):
                payload = self.worker.GenerateFeedbackRequest.model_validate(self.feedback_request())
                with patch.object(self.worker, "openai_client", fake_client(content)):
                    with self.assertRaises(HTTPException) as caught:
                        self.worker.generate_feedback(payload)
                self.assertEqual(caught.exception.status_code, 500)
                self.assertNotIn("private-canary", caught.exception.detail)

    def test_feedback_rejects_duplicate_input_ids_and_blank_prompt_fields(self):
        with self.assertRaises(ValueError):
            self.worker.GenerateFeedbackRequest.model_validate(self.feedback_request((11, 11)))
        with self.assertRaises(ValueError):
            self.worker.GenerateQuizRequest(
                context_text="  ", topic_name="aritmética", num_questions=1, is_remedial=False)
        with self.assertRaises(ValueError):
            self.worker.GenerateFeedbackRequest.model_validate({"wrong_answers": [{
                "question_id": 11,
                "topic": "aritmética",
                "question_text": "¿Cuánto es 2 + 2?",
                "student_answer": "3",
                "correct_answer": " ",
            }]})


if __name__ == "__main__":
    unittest.main()
