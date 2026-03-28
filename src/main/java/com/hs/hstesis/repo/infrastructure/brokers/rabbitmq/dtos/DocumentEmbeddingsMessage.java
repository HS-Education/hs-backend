package com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos;

import java.util.List;

public record DocumentEmbeddingsMessage(Long documentId,
                                        List<ChunkVector> chunks) {
    public DocumentEmbeddingsMessage {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document id cannot be null or negative.");
        }
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Embeddings cannot be null or empty.");
        }
    }
}
