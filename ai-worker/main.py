import json
import logging
import threading
import time
from typing import Any

import fitz
import pika
from fastapi import Depends, FastAPI, Header, HTTPException, status
from minio import Minio
from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings, SettingsConfigDict
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
    ollama_base_url: str
    ollama_model: str

settings = Settings()


class EmbedQueryRequest(BaseModel):
    text: str = Field(min_length=1, max_length=20000)


class EmbedQueryResponse(BaseModel):
    model: str
    dimensions: int
    embedding: list[float]


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
