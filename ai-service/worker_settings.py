"""Environment selection does not require any Azure credentials in local mode."""
import os
from typing import Literal
from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False, extra="ignore")
    app_env: Literal["local", "azure"] = "local"
    worker_api_key: str = Field(repr=False)
    openrouter_api_key: str = Field(repr=False)
    minio_endpoint: str = ""
    minio_root_user: str = ""
    minio_root_password: str = Field(default="", repr=False)
    minio_bucket_name: str = ""
    rabbitmq_host: str = ""
    rabbitmq_user: str = ""
    rabbitmq_password: str = Field(default="", repr=False)
    rabbitmq_port: int = 5672
    azure_storage_account_url: str = ""
    azure_documents_container: str = "documents"
    azure_results_container: str = "processing-results"
    azure_servicebus_namespace: str = ""
    azure_processing_queue: str = "document-processing"
    azure_results_queue: str = "embeddings-ready"
    azure_failures_queue: str = "document-processing-failed"
    azure_lock_renewal_seconds: int = Field(default=1800, ge=60, le=3600)
    azure_max_delivery_count: int = Field(default=5, ge=1, le=10)
    applicationinsights_connection_string: str = Field(default="", repr=False)
    openrouter_chat_model: str = "openai/gpt-oss-120b"
    openrouter_embedding_model: str = "voyageai/voyage-4-lite"
    openrouter_http_referer: str = "http://localhost:4200"
    openrouter_app_title: str = "HS Education"
    embedding_dimensions: int = Field(default=1024, ge=1024, le=1024)
    provider_max_concurrency: int = Field(default=4, ge=1, le=4)

    @model_validator(mode="after")
    def require_selected_provider(self):
        if self.app_env == "local":
            required = [self.minio_endpoint, self.minio_root_user, self.minio_root_password,
                        self.minio_bucket_name, self.rabbitmq_host, self.rabbitmq_user, self.rabbitmq_password]
        else:
            from urllib.parse import urlparse
            endpoint = urlparse(self.azure_storage_account_url)
            if (endpoint.scheme != "https" or not endpoint.hostname
                    or not endpoint.hostname.endswith(".blob.core.windows.net") or endpoint.username
                    or endpoint.query or endpoint.fragment or endpoint.path not in ("", "/")
                    or endpoint.port not in (None, 443)
                    or not self.azure_servicebus_namespace.endswith(".servicebus.windows.net")
                    or "/" in self.azure_servicebus_namespace or ":" in self.azure_servicebus_namespace):
                raise ValueError("Azure storage and messaging endpoints are required")
            required = [self.worker_api_key, self.openrouter_api_key]
            if any(value.startswith("@Microsoft.KeyVault(") for value in required):
                raise ValueError("Azure secret references must be resolved before startup")
        if not all(required):
            raise ValueError("Missing configuration for the selected environment")
        return self


def load_settings():
    # Azure never reads a developer's .env, including one accidentally copied with a package.
    return Settings(_env_file=None if os.getenv("APP_ENV", "local") == "azure" else ".env")
