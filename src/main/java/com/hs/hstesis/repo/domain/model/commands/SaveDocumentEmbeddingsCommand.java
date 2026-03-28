package com.hs.hstesis.repo.domain.model.commands;

import com.hs.hstesis.repo.domain.model.valueobjects.ChunkEmbeddingData;

import java.util.List;

public record SaveDocumentEmbeddingsCommand(Long documentId, List<ChunkEmbeddingData> chunks) {
    public SaveDocumentEmbeddingsCommand {
        if  (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document id cannot be null or negative.");
        }
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Embeddings cannot be null or empty.");
        }
    }
}
