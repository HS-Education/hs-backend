package com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos;

public record DocumentProcessingFailureMessage(Long documentId, String errorCode) {
    public DocumentProcessingFailureMessage {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Invalid document id");
        }
    }
}
