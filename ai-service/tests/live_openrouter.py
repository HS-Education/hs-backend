"""Opt-in live check; excluded from the local quality gate."""

import json
import os

from openrouter_contract import FEEDBACK_FORMAT, create_client


def main() -> None:
    api_key = os.environ.get("OPENROUTER_API_KEY")
    if not api_key:
        raise SystemExit("Set OPENROUTER_API_KEY to run the optional live check")
    model = os.environ.get("OPENROUTER_CHAT_MODEL", "openai/gpt-oss-120b")
    client = create_client(
        api_key,
        os.environ.get("OPENROUTER_HTTP_REFERER", "http://localhost:4200"),
        os.environ.get("OPENROUTER_APP_TITLE", "HS Education"),
    ).with_options(timeout=30, max_retries=0)
    with client:
        completion = client.chat.completions.create(
            model=model,
            messages=[{"role": "user", "content": "Return a short Spanish feedback for synthetic question 1."}],
            response_format=FEEDBACK_FORMAT,
            max_tokens=120,
            temperature=0,
        )
    response = json.loads(completion.choices[0].message.content or "")
    assert isinstance(response.get("feedbacks"), list)
    assert all(isinstance(item.get("questionId"), int) and isinstance(item.get("feedback"), str)
               for item in response["feedbacks"])
    print("OpenRouter live structured response: OK")


if __name__ == "__main__":
    main()
