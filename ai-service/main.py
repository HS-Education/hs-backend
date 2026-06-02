import json
import logging
import threading
import time
from typing import Any
import urllib.error
import urllib.request

import fitz
import pika
from fastapi import Depends, FastAPI, Header, HTTPException, status
from fastapi.responses import StreamingResponse
from minio import Minio
from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings, SettingsConfigDict
from groq import Groq

import sys
import types
if "transformers.onnx" not in sys.modules:
    onnx_mod = types.ModuleType("transformers.onnx")
    onnx_mod.OnnxConfig = type("OnnxConfig", (object,), {})
    sys.modules["transformers.onnx"] = onnx_mod

from transformers import AutoModel

CHUNK_SIZE = 1000
OVERLAP = 200

DOCUMENT_PROCESSING_QUEUE = "document_processing_queue"
EMBEDDINGS_READY_QUEUE = "embeddings_ready_queue"
EMBEDDING_MODEL = "jinaai/jina-embeddings-v2-base-es"

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
    groq_api_key: str

settings = Settings()


class EmbedQueryRequest(BaseModel):
    text: str = Field(min_length=1, max_length=20000)

class EmbedQueryResponse(BaseModel):
    model: str
    dimensions: int
    embedding: list[float]

class Message(BaseModel):
    role: str
    content: str

class GenerateRequest(BaseModel):
    messages: list[Message]
    context_chunks: list[str] = Field(default_factory=list)
    max_tokens: int = Field(default=512, ge=64, le=2048)
    temperature: float = Field(default=0.2, ge=0.0, le=1.5)
    stream: bool = Field(default=False)

class GenerateResponse(BaseModel):
    model: str
    answer: str
    prompt_eval_count: int | None = None
    eval_count: int | None = None


app = FastAPI(
    title="RAG Engine - AI Inference Worker",
    version="1.0",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
)

logger.info("Loading " + EMBEDDING_MODEL + " model into memory...")
model = AutoModel.from_pretrained(
    EMBEDDING_MODEL,
    trust_remote_code=True,
)
logger.info("Model loaded successfully")

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


def process_document(ch, method, properties, body: bytes) -> None:
    message: dict[str, Any] = json.loads(body)
    document_id = message.get("documentId")
    object_key = message.get("objectKey")

    logger.info("Processing documentId=%s objectKey=%s", document_id, object_key)

    try:
        logger.info("-> Downloading document from MinIO...")
        response = minio_client.get_object(
            bucket_name=settings.minio_bucket_name,
            object_name=object_key,
        )
        pdf_bytes = response.read()
        response.close()
        response.release_conn()

        logger.info("-> Extracting text and chunking...")
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")

        chunks_metadata: list[dict[str, Any]] = []
        texts_to_encode: list[str] = []
        chunk_index = 0

        for page_num in range(len(doc)):
            page = doc[page_num]
            text = page.get_text().strip()
            if not text:
                continue

            start = 0
            while start < len(text):
                end = min(start + CHUNK_SIZE, len(text))
                chunk_text = text[start:end].strip()

                if len(chunk_text) > 50:
                    texts_to_encode.append(chunk_text)
                    chunks_metadata.append(
                        {
                            "pageNumber": page_num + 1,
                            "chunkIndex": chunk_index,
                            "content": chunk_text,
                        }
                    )
                    chunk_index += 1

                start += (CHUNK_SIZE - OVERLAP)

        doc.close()

        if texts_to_encode:
            logger.info("-> Generating embeddings for %d chunks...", len(texts_to_encode))
            embeddings = model.encode(texts_to_encode).tolist()
            for i, metadata in enumerate(chunks_metadata):
                metadata["embedding"] = embeddings[i]
        else:
            logger.info("No valid text chunks found; sending empty chunk list")

        result_payload = {
            "documentId": document_id,
            "chunks": chunks_metadata,
        }

        ch.basic_publish(
            exchange="",
            routing_key=EMBEDDINGS_READY_QUEUE,
            body=json.dumps(result_payload),
            properties=pika.BasicProperties(delivery_mode=2),
        )

        ch.basic_ack(delivery_tag=method.delivery_tag)
        logger.info("Document %s processed successfully", document_id)

    except Exception:
        logger.exception("Failed processing document %s", document_id)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


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
                heartbeat=30,
                blocked_connection_timeout=120,
            )

            connection = pika.BlockingConnection(parameters)
            channel = connection.channel()

            channel.queue_declare(queue=DOCUMENT_PROCESSING_QUEUE, durable=True)
            channel.queue_declare(queue=EMBEDDINGS_READY_QUEUE, durable=True)

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
    if context_chunks:
        context_text = "\n\n".join(
            [f"[Contexto {i + 1}]\n{chunk}" for i, chunk in enumerate(context_chunks)]
        )
    else:
        context_text = "No recovered context was provided."

    return (
        "You are an academic assistant. Answer ONLY based on the context.\n"
        "If the context does not contain the answer, state it explicitly.\n\n"
        f"Context:\n{context_text}"
    )


def call_groq_generate(system_prompt: str, messages: list[dict], max_tokens: int, temperature: float) -> dict:
    client = Groq(api_key=settings.groq_api_key)
    
    groq_messages = [{"role": "system", "content": system_prompt}]
    groq_messages.extend(messages)
    
    completion = client.chat.completions.create(
        model="openai/gpt-oss-120b",
        messages=groq_messages,
        temperature=temperature,
        max_completion_tokens=max_tokens,
        top_p=1,
        reasoning_effort="medium",
        stream=False,
        stop=None
    )
    
    answer = completion.choices[0].message.content or ""
    return {
        "model": "openai/gpt-oss-120b",
        "answer": answer,
        "prompt_eval_count": completion.usage.prompt_tokens if hasattr(completion, "usage") and completion.usage else None,
        "eval_count": completion.usage.completion_tokens if hasattr(completion, "usage") and completion.usage else None,
    }

def call_groq_generate_stream(system_prompt: str, messages: list[dict], max_tokens: int, temperature: float):
    client = Groq(api_key=settings.groq_api_key)
    
    groq_messages = [{"role": "system", "content": system_prompt}]
    groq_messages.extend(messages)
    
    completion = client.chat.completions.create(
        model="openai/gpt-oss-120b",
        messages=groq_messages,
        temperature=temperature,
        max_completion_tokens=max_tokens,
        top_p=1,
        reasoning_effort="medium",
        stream=True,
        stop=None
    )
    
    for chunk in completion:
        if chunk.choices and chunk.choices[0].delta and chunk.choices[0].delta.content is not None:
            yield f"data: {chunk.choices[0].delta.content}\n\n"
    yield "data: [DONE]\n\n"


@app.on_event("startup")
def startup_event() -> None:
    rabbit_thread = threading.Thread(target=start_rabbitmq_consumer, daemon=True)
    rabbit_thread.start()


@app.get("/health", dependencies=[Depends(require_api_key)])
def health_check() -> dict[str, str]:
    return {"status": "ok", "model": EMBEDDING_MODEL}

@app.post("/embed-query", response_model=EmbedQueryResponse, dependencies=[Depends(require_api_key)])
def embed_query(payload: EmbedQueryRequest) -> EmbedQueryResponse:
    text = payload.text.strip()
    if not text:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Text must not be blank",
        )

    try:
        vector = model.encode([text])[0]
        if hasattr(vector, "tolist"):
            vector = vector.tolist()

        return EmbedQueryResponse(
            model=EMBEDDING_MODEL,
            dimensions=len(vector),
            embedding=vector,
        )
    except Exception:
        logger.exception("Failed to generate query embedding")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate embedding",
        )

@app.post("/generate", response_model=GenerateResponse, dependencies=[Depends(require_api_key)])
def generate_answer(payload: GenerateRequest) -> GenerateResponse:
    try:
        system_prompt = build_system_prompt(payload.context_chunks)
        messages_dicts = [{"role": m.role, "content": m.content} for m in payload.messages]
        
        response_dict = call_groq_generate(
            system_prompt=system_prompt,
            messages=messages_dicts,
            max_tokens=payload.max_tokens,
            temperature=payload.temperature
        )
        return GenerateResponse(**response_dict)
    except Exception as e:
        logger.exception("Error calling Groq API")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Groq API error: {str(e)}",
        )

@app.post("/generate-stream", dependencies=[Depends(require_api_key)])
def generate_answer_stream(payload: GenerateRequest) -> StreamingResponse:
    try:
        system_prompt = build_system_prompt(payload.context_chunks)
        messages_dicts = [{"role": m.role, "content": m.content} for m in payload.messages]
        
        return StreamingResponse(
            call_groq_generate_stream(system_prompt, messages_dicts, payload.max_tokens, payload.temperature),
            media_type="text/event-stream"
        )
    except Exception as e:
        logger.exception("Error calling Groq API for stream")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Groq API stream error: {str(e)}",
        )

class GenerateQuizRequest(BaseModel):
    context_text: str
    topic_name: str
    num_questions: int
    is_remedial: bool

class QuizQuestion(BaseModel):
    text: str
    options: list[str]
    correctOptionIndex: int
    isRemedial: bool

class GenerateQuizResponse(BaseModel):
    questions: list[QuizQuestion]

@app.post("/generate-quiz", response_model=GenerateQuizResponse, dependencies=[Depends(require_api_key)])
def generate_quiz(payload: GenerateQuizRequest) -> GenerateQuizResponse:
    try:
        client = Groq(api_key=settings.groq_api_key)
        
        system_prompt = (
            "You are an expert teacher. Generate a multiple choice quiz in Spanish based on the provided context.\n"
            f"Topic: {payload.topic_name}\n"
            f"Number of questions: {payload.num_questions}\n"
            f"Is remedial (needs extra explanation focus): {payload.is_remedial}\n"
            "Return ONLY a JSON object with a list of questions under the key 'questions'.\n"
            "Each question MUST have:\n"
            "- 'text': The question text\n"
            "- 'options': A list of exactly 4 string options\n"
            "- 'correctOptionIndex': An integer from 0 to 3\n"
            f"- 'isRemedial': {str(payload.is_remedial).lower()}\n"
        )
        
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Context: {payload.context_text}\n\nGenerate the JSON now."}
        ]
        
        completion = client.chat.completions.create(
            model="openai/gpt-oss-120b",
            messages=messages,
            response_format={"type": "json_object"},
            temperature=0.2,
        )
        
        content = completion.choices[0].message.content
        data = json.loads(content)
        
        questions = []
        for q in data.get("questions", []):
            questions.append(QuizQuestion(
                text=q["text"],
                options=q["options"],
                correctOptionIndex=q["correctOptionIndex"],
                isRemedial=payload.is_remedial
            ))
            
        return GenerateQuizResponse(questions=questions)
        
    except Exception as e:
        logger.exception("Error calling Groq API for quiz generation")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Groq API error: {str(e)}",
        )

class WrongQuestionFeedbackRequest(BaseModel):
    question_id: int
    topic: str
    question_text: str
    student_answer: str
    correct_answer: str

class GenerateFeedbackRequest(BaseModel):
    wrong_answers: list[WrongQuestionFeedbackRequest]

class QuestionFeedbackResponse(BaseModel):
    questionId: int
    feedback: str

class GenerateFeedbackResponse(BaseModel):
    feedbacks: list[QuestionFeedbackResponse]

@app.post("/generate-feedback", response_model=GenerateFeedbackResponse, dependencies=[Depends(require_api_key)])
def generate_feedback(payload: GenerateFeedbackRequest) -> GenerateFeedbackResponse:
    try:
        client = Groq(api_key=settings.groq_api_key)
        
        system_prompt = (
            "You are an empathetic, expert teacher. The student just finished a quiz and got some questions wrong.\n"
            "For each wrong question, generate a short, encouraging feedback paragraph in Spanish (around 30 to 50 words) explaining why their answer is wrong and the correct answer is right. You may include a very brief example if it's a complex topic.\n"
            "Return ONLY a JSON object with a list of feedbacks under the key 'feedbacks'.\n"
            "Each item in the list MUST have:\n"
            "- 'questionId': The integer ID of the question\n"
            "- 'feedback': The feedback text\n"
        )
        
        wrong_details = ""
        for w in payload.wrong_answers:
            wrong_details += f"- Question ID: {w.question_id}. Topic: {w.topic}. Question: {w.question_text}. They answered: {w.student_answer}. Correct was: {w.correct_answer}.\n"
            
        user_message = "The student answered everything correctly!"
        if wrong_details:
            user_message = f"The student got these wrong:\n{wrong_details}\nGenerate the JSON now."

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_message}
        ]
        
        if not payload.wrong_answers:
            return GenerateFeedbackResponse(feedbacks=[])

        completion = client.chat.completions.create(
            model="openai/gpt-oss-120b",
            messages=messages,
            response_format={"type": "json_object"},
            temperature=0.5,
        )
        
        content = completion.choices[0].message.content
        data = json.loads(content)
        
        feedbacks = []
        for f in data.get("feedbacks", []):
            feedbacks.append(QuestionFeedbackResponse(
                questionId=f["questionId"],
                feedback=f["feedback"]
            ))
            
        return GenerateFeedbackResponse(feedbacks=feedbacks)
        
    except Exception as e:
        logger.exception("Error calling Groq API for feedback generation")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Groq API error: {str(e)}",
        )
