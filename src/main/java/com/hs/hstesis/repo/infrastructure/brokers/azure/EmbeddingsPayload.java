package com.hs.hstesis.repo.infrastructure.brokers.azure;

import com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos.ChunkVector;
import java.util.List;

public record EmbeddingsPayload(long documentId, int generation, List<ChunkVector> chunks) {
    public void validateAgainst(EmbeddingsReference reference) {
        if (documentId != reference.documentId() || generation != reference.generation() || chunks == null
                || chunks.size() != reference.chunkCount()) throw new IllegalArgumentException("Embeddings metadata mismatch");
        int totalCharacters = 0;
        for (int index = 0; index < chunks.size(); index++) {
            var chunk = chunks.get(index);
            if (chunk == null || chunk.pageNumber() > 300 || chunk.chunkIndex() != index
                    || chunk.embedding().length != reference.dimensions()) throw new IllegalArgumentException("Invalid embedding chunk");
            totalCharacters += chunk.content().length();
            for (float number : chunk.embedding()) {
                if (!Float.isFinite(number)) throw new IllegalArgumentException("Invalid embedding number");
            }
        }
        if (totalCharacters > 1_000_000) throw new IllegalArgumentException("Embeddings text exceeds limit");
    }
}
