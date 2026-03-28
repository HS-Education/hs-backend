package com.hs.hstesis.repo.domain.model.valueobjects;

public record ChunkEmbeddingData(Integer pageNumber,
                                 Integer chunkIndex,
                                 String content,
                                 float[] embedding) {
    public ChunkEmbeddingData {
        if (pageNumber == null || pageNumber <= 0) {
            throw new IllegalArgumentException("Page number cannot be null or negative.");
        }
        if (chunkIndex == null || chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index cannot be null or negative.");
        }
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty.");
        }
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding cannot be null or empty.");
        }
    }
}
