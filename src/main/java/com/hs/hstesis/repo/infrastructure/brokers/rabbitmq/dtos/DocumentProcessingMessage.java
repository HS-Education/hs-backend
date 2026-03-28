package com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos;

public record DocumentProcessingMessage(Long documentId, String objectKey) {
    public DocumentProcessingMessage {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document id cannot be null or negative");
        }
        if (objectKey == null || objectKey.isEmpty()) {
            throw new IllegalArgumentException("Object key cannot be null or empty");
        }
    }
}
