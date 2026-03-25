import json
import threading
import fitz
import pika
from fastapi import FastAPI
from minio import Minio
from transformers import AutoModel
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    minio_endpoint: str
    minio_root_user: str
    minio_root_password: str
    minio_bucket_name: str
    rabbitmq_host: str
    rabbitmq_user: str
    rabbitmq_password: str

    class Config:
        env_file = ".env"

settings = Settings()

app = FastAPI(title="RAG Engine - AI Inference Worker", version="1.0")

print("Loading Jina AI model into memory (This might take a while on first run)...")
model = AutoModel.from_pretrained("jinaai/jina-embeddings-v2-base-es", trust_remote_code=True)
print("Model loaded successfully!")

minio_client = Minio(
    settings.minio_endpoint,
    access_key=settings.minio_root_user,
    secret_key=settings.minio_root_password,
    secure=False
)

def process_document(ch, method, properties, body):

    message = json.loads(body)
    document_id = message.get("documentId")
    object_key = message.get("objectKey")

    CHUNK_SIZE = 1000
    OVERLAP = 200

    print(f"\nProcessing Document ID: {document_id} | Key: {object_key}")

    try:
        # Download PDF from MinIO
        print("   -> Downloading from MinIO...")
        response = minio_client.get_object(
            bucket_name=settings.minio_bucket_name,
            object_name=object_key
        )
        pdf_bytes = response.read()
        response.close()
        response.release_conn()

        # Extract text and chunk it
        print("   -> Extracting text and chunking...")
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")

        chunks_metadata = []
        texts_to_encode = []
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
                    chunks_metadata.append({
                        "pageNumber": page_num + 1,
                        "chunkIndex": chunk_index,
                        "content": chunk_text
                    })
                    chunk_index += 1

                start += (CHUNK_SIZE - OVERLAP)

        doc.close()

        # Generate vectors for all chunks at once (BATCH processing)
        print(f"   -> Generating vectors for {len(texts_to_encode)} chunks...")
        embeddings = model.encode(texts_to_encode).tolist()

        for i, metadata in enumerate(chunks_metadata):
            metadata["embedding"] = embeddings[i]

        # Package results and send back to RabbitMQ
        print("   -> Sending results to return queue...")
        result_payload = {
            "documentId": document_id,
            "chunks": chunks_metadata
        }

        ch.basic_publish(
            exchange='',
            routing_key='embeddings_ready_queue',
            body=json.dumps(result_payload)
        )

        print(f"[SUCCESS] Document {document_id} fully processed and returned.")

        ch.basic_ack(delivery_tag=method.delivery_tag)

    except Exception as e:
        print(f"[ERROR] Failed processing document {document_id}: {str(e)}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

def start_rabbitmq_consumer():

    credentials = pika.PlainCredentials(settings.rabbitmq_user, settings.rabbitmq_password)
    parameters = pika.ConnectionParameters(settings.rabbitmq_host, 5672, '/', credentials)

    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()

    channel.queue_declare(queue='document_processing_queue', durable=True)
    channel.queue_declare(queue='embeddings_ready_queue', durable=True)

    channel.basic_consume(queue='document_processing_queue', on_message_callback=process_document)

    print("RabbitMQ Consumer listening on 'document_processing_queue'...")
    channel.start_consuming()

@app.on_event("startup")
def startup_event():
    rabbit_thread = threading.Thread(target=start_rabbitmq_consumer, daemon=True)
    rabbit_thread.start()

@app.get("/health")
def health_check():
    return {"status": "ok", "model": "jina-embeddings-v2-base-es"}