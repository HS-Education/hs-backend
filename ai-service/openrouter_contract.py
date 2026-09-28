"""OpenRouter request contract shared by production and offline tests."""

from __future__ import annotations

from typing import Any

from openai import OpenAI


def create_client(api_key: str, referer: str, title: str, http_client: Any = None) -> OpenAI:
    options: dict[str, Any] = {
        "api_key": api_key,
        "base_url": "https://openrouter.ai/api/v1",
        "default_headers": {
            "HTTP-Referer": referer,
            "X-OpenRouter-Title": title,
        },
    }
    if http_client is not None:
        options["http_client"] = http_client
    return OpenAI(**options)


def json_schema_format(name: str, item_schema: dict[str, Any], collection_key: str) -> dict[str, Any]:
    return {
        "type": "json_schema",
        "json_schema": {
            "name": name,
            "strict": True,
            "schema": {
                "type": "object",
                "properties": {collection_key: {"type": "array", "items": item_schema}},
                "required": [collection_key],
                "additionalProperties": False,
            },
        },
    }


QUIZ_FORMAT = json_schema_format("quiz", {
    "type": "object",
    "properties": {
        "text": {"type": "string"},
        "options": {"type": "array", "items": {"type": "string"}},
        "correctOptionIndex": {"type": "integer"},
        "isRemedial": {"type": "boolean"},
        "difficulty": {"type": "string", "enum": ["LOW", "INTERMEDIATE"]},
    },
    "required": ["text", "options", "correctOptionIndex", "isRemedial", "difficulty"],
    "additionalProperties": False,
}, "questions")

FEEDBACK_FORMAT = json_schema_format("feedback", {
    "type": "object",
    "properties": {
        "questionId": {"type": "integer"},
        "feedback": {"type": "string"},
    },
    "required": ["questionId", "feedback"],
    "additionalProperties": False,
}, "feedbacks")
