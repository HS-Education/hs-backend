import json
import logging
import threading
import time
from typing import Any, Literal
import urllib.error
import urllib.request

import fitz
import pika
from fastapi import Depends, FastAPI, Header, HTTPException, status, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import StreamingResponse, JSONResponse
from minio import Minio
from openrouter_contract import FEEDBACK_FORMAT, QUIZ_FORMAT, create_client
from pydantic import BaseModel, Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

CHUNK_SIZE = 1000
OVERLAP = 200

DOCUMENT_PROCESSING_QUEUE = "document_processing_queue"
EMBEDDINGS_READY_QUEUE = "embeddings_ready_queue"

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
    role: str
    content: str

class GenerateRequest(BaseModel):
    messages: list[Message]
    context_chunks: list[str] = Field(default_factory=list)
    max_tokens: int = Field(default=2048, ge=64, le=4096)
    temperature: float = Field(default=0.2, ge=0.0, le=1.5)
    stream: bool = Field(default=False)

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
    body = await request.body()
    logger.error(
        "422 Validation error on %s %s: %s - raw_body: %s",
        request.method,
        request.url.path,
        exc.errors(),
        body.decode("utf-8", errors="replace"),
    )
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={"detail": exc.errors(), "body": body.decode("utf-8", errors="replace")},
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
    response = openai_client.embeddings.create(
        model=settings.openrouter_embedding_model,
        input=texts,
        dimensions=settings.embedding_dimensions,
    )
    # Sort results by index to ensure original order is preserved
    sorted_items = sorted(response.data, key=lambda x: x.index)
    embeddings = [item.embedding for item in sorted_items]
    invalid_dimensions = [len(embedding) for embedding in embeddings if len(embedding) != settings.embedding_dimensions]
    if invalid_dimensions:
        raise RuntimeError(
            "OpenRouter returned embeddings with unexpected dimensions "
            f"{invalid_dimensions[0]}; expected {settings.embedding_dimensions}."
        )
    return embeddings


def process_document(ch, method, properties, body: bytes) -> None:
    message: dict[str, Any] = json.loads(body)
    document_id = message.get("documentId")
    object_key = message.get("objectKey")

    logger.info("Processing documentId=%s objectKey=%s", document_id, object_key)

    try:
        t0_total = time.time()
        
        logger.info("-> Downloading document from MinIO...")
        t0_download = time.time()
        response = minio_client.get_object(
            bucket_name=settings.minio_bucket_name,
            object_name=object_key,
        )
        pdf_bytes = response.read()
        response.close()
        response.release_conn()
        t1_download = time.time()

        logger.info("-> Extracting text and chunking...")
        t0_extract = time.time()
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
        t1_extract = time.time()

        if texts_to_encode:
            logger.info("-> Generating embeddings for %d chunks via OpenRouter (%s)...", len(texts_to_encode), settings.openrouter_embedding_model)
            t0_embed = time.time()

            # Process in batches of 50 to respect API limits if the document is very large
            batch_size = 50
            all_embeddings: list[list[float]] = []
            for i in range(0, len(texts_to_encode), batch_size):
                batch = texts_to_encode[i : i + batch_size]
                batch_embeddings = get_embeddings(batch)
                all_embeddings.extend(batch_embeddings)

            for i, metadata in enumerate(chunks_metadata):
                metadata["embedding"] = all_embeddings[i]
            t1_embed = time.time()
            logger.info("Embeddings generated in %.3f seconds", t1_embed - t0_embed)
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
        t1_total = time.time()
        logger.info("Document %s processed successfully! [TOTAL TIME: %.3f seconds]", document_id, t1_total - t0_total)

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
                heartbeat=1800,
                blocked_connection_timeout=1800,
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
        context_text = "No hay contexto proporcionado."

    return (
        "Eres Sery, un asistente acad├®mico c├ílido, amable y servicial.\n"
        "Sigue estrictamente estas reglas:\n"
        "1. Si el usuario simplemente te saluda (ej. 'Hola', 'Buenos d├¡as'), devu├®lvele el saludo cordialmente y ofr├®cele tu ayuda present├índote como Sery. NO hables sobre el contexto si solo te est├ín saludando.\n"
        "2. Si el usuario te hace una pregunta del curso, responde siempre con amabilidad bas├índote ├ÜNICAMENTE en el contexto proporcionado abajo. SIEMPRE debes incluir al final de tu respuesta el nombre de la 'Fuente' y el 'Enlace de descarga' exacto de donde sacaste la informaci├│n (esa informaci├│n viene dentro del contexto proporcionado).\n"
        "3. Si hace una pregunta y la respuesta no est├í en el contexto, disc├║lpate amablemente y dile que no tienes informaci├│n sobre ese tema espec├¡fico en tus documentos. No inventes respuestas ni pongas enlaces falsos.\n"
        "4. Si el usuario te pide expl├¡citamente generar un cuestionario, examen o preguntas para evaluar, debes responder ├ÜNICAMENTE con un bloque de c├│digo JSON con `type: \"questionnaire_draft\"` y un arreglo `questions` que contenga `text`, `options` (array de strings) y `correctOptionIndex` (n├║mero). NO agregues ning├║n otro texto fuera del JSON.\n\n"
        f"Contexto:\n{context_text}"
    )


def call_openrouter_generate(system_prompt: str, messages: list[dict], max_tokens: int, temperature: float) -> dict:
    llm_messages = [{"role": "system", "content": system_prompt}]
    llm_messages.extend(messages)
    
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
    
    completion = openai_client.chat.completions.create(
        model=settings.openrouter_chat_model,
        messages=llm_messages,
        temperature=temperature,
        max_tokens=max_tokens,
        stream=True,
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
    except Exception:
        logger.exception("Failed to generate query embedding via OpenRouter")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate embedding",
        )

@app.post("/generate", response_model=GenerateResponse, dependencies=[Depends(require_api_key)])
def generate_answer(payload: GenerateRequest) -> GenerateResponse:
    try:
        system_prompt = build_system_prompt(payload.context_chunks)
        messages_dicts = [{"role": m.role, "content": m.content} for m in payload.messages]
        
        response_dict = call_openrouter_generate(
            system_prompt=system_prompt,
            messages=messages_dicts,
            max_tokens=payload.max_tokens,
            temperature=payload.temperature,
        )
        return GenerateResponse(**response_dict)
    except Exception as e:
        logger.exception("Error calling OpenRouter API")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"OpenRouter API error: {str(e)}",
        )

@app.post("/generate-stream", dependencies=[Depends(require_api_key)])
def generate_answer_stream(payload: GenerateRequest) -> StreamingResponse:
    try:
        system_prompt = build_system_prompt(payload.context_chunks)
        messages_dicts = [{"role": m.role, "content": m.content} for m in payload.messages]
        
        return StreamingResponse(
            call_openrouter_generate_stream(system_prompt, messages_dicts, payload.max_tokens, payload.temperature),
            media_type="text/event-stream",
        )
    except Exception as e:
        logger.exception("Error calling OpenRouter API for stream")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"OpenRouter API stream error: {str(e)}",
        )

class GenerateQuizRequest(BaseModel):
    context_text: str
    topic_name: str
    num_questions: int
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
            "- 'difficulty': either 'LOW' or 'INTERMEDIATE'\n"
            "For a 15-question bank, generate exactly 5 LOW questions and 10 INTERMEDIATE questions. "
            "LOW questions evaluate essential definitions and direct application; INTERMEDIATE questions "
            "require interpretation, comparison or multi-step application.\n"
        )
        
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Context: {payload.context_text}\n\nGenerate the JSON now."}
        ]
        
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
        
    except Exception as e:
        logger.error("OpenRouter quiz generation failed: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="OpenRouter quiz response was unavailable or invalid",
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
    feedback: str = Field(min_length=1)

class GenerateFeedbackResponse(BaseModel):
    feedbacks: list[QuestionFeedbackResponse]

@app.post("/generate-feedback", response_model=GenerateFeedbackResponse, dependencies=[Depends(require_api_key)])
def generate_feedback(payload: GenerateFeedbackRequest) -> GenerateFeedbackResponse:
    try:
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
        
    except Exception as e:
        logger.error("OpenRouter feedback generation failed: %s", type(e).__name__)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="OpenRouter feedback response was unavailable or invalid",
        )
