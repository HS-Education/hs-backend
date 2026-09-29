import json
import logging
import threading
import time
from typing import Any, Literal
import urllib.error
import urllib.request

import fitz
import pika
from safety import (
    SYSTEM_PROMPT, UNTRUSTED_DATA_RULE, MAX_PDF_BYTES, MAX_REQUEST_CHARS,
    CapacityExceeded, CapacityLimit, build_messages, extract_chunks, validate_embeddings, sse_token_frame,
)
from fastapi import Depends, FastAPI, Header, HTTPException, status, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import StreamingResponse, JSONResponse
from minio import Minio
from openrouter_contract import FEEDBACK_FORMAT, QUIZ_FORMAT, create_client
from pydantic import BaseModel, Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

DOCUMENT_PROCESSING_QUEUE = "document_processing_queue"
EMBEDDINGS_READY_QUEUE = "embeddings_ready_queue"
DOCUMENT_PROCESSING_FAILED_QUEUE = "document_processing_failed_queue"
provider_capacity = CapacityLimit(4)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [ai-worker] %(message)s",
)
logger = logging.getLogger(__name__)


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False)

    worker_api_key: str
    minio_endpoint: str
    minio_root_user: str
    minio_root_password: str
    minio_bucket_name: str
    rabbitmq_host: str
    rabbitmq_user: str
    rabbitmq_password: str
    rabbitmq_port: str
    openrouter_api_key: str
    openrouter_chat_model: str = "openai/gpt-oss-120b"
    openrouter_embedding_model: str = "voyageai/voyage-4-lite"
    openrouter_http_referer: str = "http://localhost:4200"
    openrouter_app_title: str = "HS Education"
    embedding_dimensions: int = 1024

settings = Settings()


class EmbedQueryRequest(BaseModel):
    text: str = Field(default="", max_length=20000)
    query: str | None = None
    question: str | None = None

    @model_validator(mode="before")
    @classmethod
    def resolve_text_field(cls, data: Any) -> Any:
        if isinstance(data, dict):
            if not data.get("text"):
                data["text"] = data.get("query") or data.get("question") or ""
        return data

class EmbedQueryResponse(BaseModel):
    model: str
    dimensions: int
    embedding: list[float]

class Message(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(max_length=20000)

class GenerateRequest(BaseModel):
    messages: list[Message] = Field(max_length=100)
    context_chunks: list[str] = Field(default_factory=list, max_length=25)
    max_tokens: int = Field(default=2048, ge=64, le=4096)
    temperature: float = Field(default=0.2, ge=0.0, le=1.5)
    stream: bool = Field(default=False)

    @model_validator(mode="after")
    def bound_prompt(self):
        if sum(len(m.content) for m in self.messages) + sum(len(c) for c in self.context_chunks) > MAX_REQUEST_CHARS:
            raise ValueError("Request text is too large")
        return self

class GenerateResponse(BaseModel):
    model: str
    answer: str
    prompt_eval_count: int | None = None
    eval_count: int | None = None


app = FastAPI(
    title="RAG Engine - AI Inference Worker",
    version="2.0",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
)

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.warning("Invalid request on %s %s", request.method, request.url.path)
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={"detail": "Invalid request payload"},
    )

logger.info(
    "Initializing OpenRouter client with chat_model='%s' and embedding_model='%s' (dims=%d)...",
    settings.openrouter_chat_model,
    settings.openrouter_embedding_model,
    settings.embedding_dimensions,
)

openai_client = create_client(
    settings.openrouter_api_key,
    settings.openrouter_http_referer,
    settings.openrouter_app_title,
)

minio_client = Minio(
    settings.minio_endpoint,
    access_key=settings.minio_root_user,
    secret_key=settings.minio_root_password,
    secure=False,
)

def require_api_key(x_api_key: str | None = Header(default=None)) -> None:
    if x_api_key != settings.worker_api_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized",
        )


def get_embeddings(texts: list[str]) -> list[list[float]]:
    """
    Generates embeddings for a list of texts using the configured OpenRouter embedding model.
    Handles batching if needed.
    """
    if not texts:
        return []

    # OpenRouter / OpenAI embeddings API call
    with provider_capacity.acquire():
        response = openai_client.embeddings.create(
            model=settings.openrouter_embedding_model,
            input=texts,
            dimensions=settings.embedding_dimensions,
        )
    return validate_embeddings(response.data, len(texts), settings.embedding_dimensions)


def process_document(ch, method, properties, body: bytes) -> None:
    document_id = None
    try:
        message = json.loads(body)
        if not isinstance(message, dict):
            raise ValueError("Invalid document message")
        document_id = message.get("documentId")
        object_key = message.get("objectKey")
        if not isinstance(document_id, int) or document_id <= 0 or not isinstance(object_key, str) or not object_key.startswith("documents/"):
            raise ValueError("Invalid document message")

        response = minio_client.get_object(
            bucket_name=settings.minio_bucket_name, object_name=object_key,
        )
        try:
            pdf_bytes = response.read(MAX_PDF_BYTES + 1)
        finally:
            response.close()
            response.release_conn()
        chunks_metadata = extract_chunks(pdf_bytes)
        for index in range(0, len(chunks_metadata), 50):
            batch = chunks_metadata[index:index + 50]
            embeddings = get_embeddings([chunk["content"] for chunk in batch])
            for metadata, vector in zip(batch, embeddings, strict=True):
                metadata["embedding"] = vector

        result_payload = {"documentId": document_id, "chunks": chunks_metadata}
        if ch.basic_publish(
            exchange="", routing_key=EMBEDDINGS_READY_QUEUE,
            body=json.dumps(result_payload), properties=pika.BasicProperties(delivery_mode=2),
        ) is False:
            raise RuntimeError("Embeddings publication was not confirmed")
        ch.basic_ack(delivery_tag=method.delivery_tag)
        logger.info("Document processing finished for documentId=%s chunks=%s", document_id, len(chunks_metadata))
    except Exception as error:
        logger.error("Document processing failed for documentId=%s errorType=%s", document_id, type(error).__name__)
        if document_id is None:
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            return
        try:
            code = "DOCUMENT_INVALID" if isinstance(error, (ValueError, fitz.FileDataError)) else "PROCESSING_UNAVAILABLE"
            if ch.basic_publish(
                exchange="", routing_key=DOCUMENT_PROCESSING_FAILED_QUEUE,
                body=json.dumps({"documentId": document_id, "errorCode": code}),
                properties=pika.BasicProperties(delivery_mode=2),
            ) is False:
                raise RuntimeError("Failure publication was not confirmed")
            ch.basic_ack(delivery_tag=method.delivery_tag)
        except Exception:
            logger.error("Could not publish failure for documentId=%s; requeueing", document_id)
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)


def start_rabbitmq_consumer() -> None:
    while True:
        try:
            credentials = pika.PlainCredentials(
                settings.rabbitmq_user,
                settings.rabbitmq_password,
            )
            parameters = pika.ConnectionParameters(
                host=settings.rabbitmq_host,
                port=settings.rabbitmq_port,
                virtual_host="/",
                credentials=credentials,
                heartbeat=1800,
                blocked_connection_timeout=1800,
            )

            connection = pika.BlockingConnection(parameters)
            channel = connection.channel()

            channel.queue_declare(queue=DOCUMENT_PROCESSING_QUEUE, durable=True)
            channel.queue_declare(queue=EMBEDDINGS_READY_QUEUE, durable=True)
            channel.queue_declare(queue=DOCUMENT_PROCESSING_FAILED_QUEUE, durable=True)
            channel.confirm_delivery()

            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(
                queue=DOCUMENT_PROCESSING_QUEUE,
                on_message_callback=process_document,
            )

            logger.info("RabbitMQ consumer listening on '%s'", DOCUMENT_PROCESSING_QUEUE)
            channel.start_consuming()

        except Exception:
            logger.exception("RabbitMQ connection/consumer error; retrying in 5s...")
            time.sleep(5)


def build_system_prompt(context_chunks: list[str]) -> str:
    # Retained for call sites; retrieved text is passed separately by build_messages.
    return SYSTEM_PROMPT


def call_openrouter_generate(system_prompt: str, messages: list[dict], max_tokens: int, temperature: float) -> dict:
    llm_messages = [{"role": "system", "content": system_prompt}]
    llm_messages.extend(messages)
    
    with provider_capacity.acquire():
        completion = openai_client.chat.completions.create(
            model=settings.openrouter_chat_model,
            messages=llm_messages,
            temperature=temperature,
            max_tokens=max_tokens,
            stream=False,
        )
    
    answer = completion.choices[0].message.content or ""
    return {
        "model": settings.openrouter_chat_model,
        "answer": answer,
        "prompt_eval_count": completion.usage.prompt_tokens if hasattr(completion, "usage") and completion.usage else None,
        "eval_count": completion.usage.completion_tokens if hasattr(completion, "usage") and completion.usage else None,
    }

def call_openrouter_generate_stream(system_prompt: str, messages: list[dict], max_tokens: int, temperature: float):
    llm_messages = [{"role": "system", "content": system_prompt}]
    llm_messages.extend(messages)
    
    with provider_capacity.acquire():
        completion = openai_client.chat.completions.create(
            model=settings.openrouter_chat_model,
            messages=llm_messages,
            temperature=temperature,
            max_tokens=max_tokens,
            stream=True,
        )
        for chunk in completion:
            if chunk.choices and chunk.choices[0].delta and chunk.choices[0].delta.content is not None:
                yield sse_token_frame(chunk.choices[0].delta.content)
    yield "data: [DONE]\n\n"


@app.on_event("startup")
def startup_event() -> None:
    rabbit_thread = threading.Thread(target=start_rabbitmq_consumer, daemon=True)
    rabbit_thread.start()


@app.get("/health", dependencies=[Depends(require_api_key)])
def health_check() -> dict[str, str]:
    return {
        "status": "ok",
        "chat_model": settings.openrouter_chat_model,
        "embedding_model": settings.openrouter_embedding_model,
        "dimensions": str(settings.embedding_dimensions),
    }

@app.post("/embed-query", response_model=EmbedQueryResponse, dependencies=[Depends(require_api_key)])
def embed_query(payload: EmbedQueryRequest) -> EmbedQueryResponse:
    text = payload.text.strip()
    if not text:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Text must not be blank",
        )

    try:
        embeddings = get_embeddings([text])
        vector = embeddings[0]

        return EmbedQueryResponse(
            model=settings.openrouter_embedding_model,
            dimensions=len(vector),
            embedding=vector,
        )
    except CapacityExceeded:
        raise HTTPException(status_code=503, detail="AI service is busy; retry shortly", headers={"Retry-After": "5"})
    except Exception:
        logger.error("Failed to generate query embedding via OpenRouter")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate embedding",
        )

@app.post("/generate", response_model=GenerateResponse, dependencies=[Depends(require_api_key)])
def generate_answer(payload: GenerateRequest) -> GenerateResponse:
    try:
        system_prompt = build_system_prompt(payload.context_chunks)
        messages_dicts = build_messages(payload.context_chunks, [m.model_dump() for m in payload.messages])[1:]
        
        response_dict = call_openrouter_generate(
            system_prompt=system_prompt,
            messages=messages_dicts,
            max_tokens=payload.max_tokens,
            temperature=payload.temperature,
        )
        return GenerateResponse(**response_dict)
    except CapacityExceeded:
        raise HTTPException(status_code=503, detail="AI service is busy; retry shortly", headers={"Retry-After": "5"})
    except Exception:
        logger.error("OpenRouter generation failed")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="AI response unavailable",
        )

@app.post("/generate-stream", dependencies=[Depends(require_api_key)])
def generate_answer_stream(payload: GenerateRequest) -> StreamingResponse:
    try:
        system_prompt = build_system_prompt(payload.context_chunks)
        messages_dicts = build_messages(payload.context_chunks, [m.model_dump() for m in payload.messages])[1:]
        
        return StreamingResponse(
            call_openrouter_generate_stream(system_prompt, messages_dicts, payload.max_tokens, payload.temperature),
            media_type="text/event-stream",
        )
    except Exception:
        logger.error("OpenRouter stream preparation failed")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="AI response unavailable",
        )

class GenerateQuizRequest(BaseModel):
    context_text: str = Field(max_length=100_000)
    topic_name: str = Field(max_length=200)
    num_questions: int = Field(ge=1, le=30)
    is_remedial: bool

class QuizQuestion(BaseModel):
    text: str = Field(min_length=1)
    options: list[str] = Field(min_length=4, max_length=4)
    correctOptionIndex: int = Field(ge=0, le=3)
    isRemedial: bool
    difficulty: Literal["LOW", "INTERMEDIATE"]

class GenerateQuizResponse(BaseModel):
    questions: list[QuizQuestion]

@app.post("/generate-quiz", response_model=GenerateQuizResponse, dependencies=[Depends(require_api_key)])
def generate_quiz(payload: GenerateQuizRequest) -> GenerateQuizResponse:
    try:
        system_prompt = UNTRUSTED_DATA_RULE + (
            "You are an expert teacher. Generate a multiple choice quiz in Spanish based on the provided context.\n"
            f"Number of questions: {payload.num_questions}\n"
            f"Is remedial (needs extra explanation focus): {payload.is_remedial}\n"
            "Return ONLY a JSON object with a list of questions under the key 'questions'.\n"
            "Each question MUST have:\n"
            "- 'text': The question text\n"
            "- 'options': A list of exactly 4 string options\n"
            "- 'correctOptionIndex': An integer from 0 to 3\n"
            f"- 'isRemedial': {str(payload.is_remedial).lower()}\n"
            "- 'difficulty': either 'LOW' or 'INTERMEDIATE'\n"
            "For a 15-question bank, generate exactly 5 LOW questions and 10 INTERMEDIATE questions. "
            "LOW questions evaluate essential definitions and direct application; INTERMEDIATE questions "
            "require interpretation, comparison or multi-step application.\n"
        )
        
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": json.dumps({"untrusted_topic": payload.topic_name, "untrusted_context": payload.context_text}, ensure_ascii=False)}
        ]
        
        with provider_capacity.acquire():
            completion = openai_client.chat.completions.create(
                model=settings.openrouter_chat_model,
                messages=messages,
                response_format=QUIZ_FORMAT,
                temperature=0.2,
            )
        
        content = completion.choices[0].message.content
        data = GenerateQuizResponse.model_validate_json(content)
        
        questions = []
        for q in data.questions:
            questions.append(QuizQuestion(
                text=q.text,
                options=q.options,
                correctOptionIndex=q.correctOptionIndex,
                isRemedial=payload.is_remedial,
                difficulty=q.difficulty
            ))
            
        return GenerateQuizResponse(questions=questions)
        
    except CapacityExceeded:
        raise HTTPException(status_code=503, detail="AI service is busy; retry shortly", headers={"Retry-After": "5"})
    except Exception as e:
        logger.error("OpenRouter quiz generation failed: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="OpenRouter quiz response was unavailable or invalid",
        )

class WrongQuestionFeedbackRequest(BaseModel):
    question_id: int
    topic: str = Field(max_length=200)
    question_text: str = Field(max_length=5000)
    student_answer: str = Field(max_length=2000)
    correct_answer: str = Field(max_length=2000)

class GenerateFeedbackRequest(BaseModel):
    wrong_answers: list[WrongQuestionFeedbackRequest] = Field(max_length=30)

class QuestionFeedbackResponse(BaseModel):
    questionId: int
    feedback: str = Field(min_length=1)

class GenerateFeedbackResponse(BaseModel):
    feedbacks: list[QuestionFeedbackResponse]

@app.post("/generate-feedback", response_model=GenerateFeedbackResponse, dependencies=[Depends(require_api_key)])
def generate_feedback(payload: GenerateFeedbackRequest) -> GenerateFeedbackResponse:
    try:
        system_prompt = UNTRUSTED_DATA_RULE + (
            "You are an empathetic, expert teacher. The student just finished a quiz and got some questions wrong.\n"
            "For each wrong question, generate a short, encouraging feedback paragraph in Spanish (around 30 to 50 words) explaining why their answer is wrong and the correct answer is right. You may include a very brief example if it's a complex topic.\n"
            "Return ONLY a JSON object with a list of feedbacks under the key 'feedbacks'.\n"
            "Each item in the list MUST have:\n"
            "- 'questionId': The integer ID of the question\n"
            "- 'feedback': The feedback text\n"
        )
        
        user_message = json.dumps({"untrusted_wrong_answers": [w.model_dump() for w in payload.wrong_answers]}, ensure_ascii=False)

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_message}
        ]
        
        if not payload.wrong_answers:
            return GenerateFeedbackResponse(feedbacks=[])

        with provider_capacity.acquire():
            completion = openai_client.chat.completions.create(
                model=settings.openrouter_chat_model,
                messages=messages,
                response_format=FEEDBACK_FORMAT,
                temperature=0.5,
            )
        
        content = completion.choices[0].message.content
        data = GenerateFeedbackResponse.model_validate_json(content)
        
        feedbacks = []
        for f in data.feedbacks:
            feedbacks.append(QuestionFeedbackResponse(
                questionId=f.questionId,
                feedback=f.feedback
            ))
            
        return GenerateFeedbackResponse(feedbacks=feedbacks)
        
    except CapacityExceeded:
        raise HTTPException(status_code=503, detail="AI service is busy; retry shortly", headers={"Retry-After": "5"})
    except Exception as e:
        logger.error("OpenRouter feedback generation failed: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="OpenRouter feedback response was unavailable or invalid",
        )
