import json
import unittest

import httpx
from openai import APIStatusError

from openrouter_contract import FEEDBACK_FORMAT, QUIZ_FORMAT, create_client


class OpenRouterContractTest(unittest.TestCase):
    def test_request_has_auth_attribution_and_strict_json_schema(self):
        observed = []

        def reply(request: httpx.Request) -> httpx.Response:
            observed.append(request)
            return httpx.Response(200, json={
                "id": "offline-test",
                "object": "chat.completion",
                "created": 1,
                "model": "openai/gpt-oss-120b",
                "choices": [{"index": 0, "finish_reason": "stop", "message": {
                    "role": "assistant", "content": '{"questions":[]}'
                }}],
            })

        with httpx.Client(transport=httpx.MockTransport(reply)) as transport:
            with create_client("synthetic-secret", "http://localhost:4200", "HS Education", transport) as client:
                response = client.chat.completions.create(
                    model="openai/gpt-oss-120b",
                    messages=[{"role": "user", "content": "synthetic prompt"}],
                    response_format=QUIZ_FORMAT,
                )

        self.assertEqual(json.loads(response.choices[0].message.content), {"questions": []})
        self.assertEqual(len(observed), 1)
        request = observed[0]
        self.assertEqual(request.url.path, "/api/v1/chat/completions")
        self.assertEqual(request.headers["authorization"], "Bearer synthetic-secret")
        self.assertEqual(request.headers["http-referer"], "http://localhost:4200")
        self.assertEqual(request.headers["x-openrouter-title"], "HS Education")
        sent = json.loads(request.content)
        self.assertEqual(sent["model"], "openai/gpt-oss-120b")
        self.assertEqual(sent["response_format"], QUIZ_FORMAT)
        self.assertTrue(sent["response_format"]["json_schema"]["strict"])

    def test_feedback_schema_rejects_unlisted_fields(self):
        schema = FEEDBACK_FORMAT["json_schema"]["schema"]
        self.assertFalse(schema["additionalProperties"])
        self.assertFalse(schema["properties"]["feedbacks"]["items"]["additionalProperties"])
        self.assertEqual(schema["required"], ["feedbacks"])

    def test_provider_failure_is_reported_without_live_network(self):
        def unavailable(_request: httpx.Request) -> httpx.Response:
            return httpx.Response(503, json={"error": {"message": "unavailable"}})

        with httpx.Client(transport=httpx.MockTransport(unavailable)) as transport:
            with create_client("synthetic-secret", "http://localhost:4200", "HS Education", transport) as client:
                with self.assertRaises(APIStatusError):
                    client.with_options(max_retries=0).chat.completions.create(
                        model="openai/gpt-oss-120b",
                        messages=[{"role": "user", "content": "synthetic prompt"}],
                        response_format=FEEDBACK_FORMAT,
                    )


if __name__ == "__main__":
    unittest.main()
