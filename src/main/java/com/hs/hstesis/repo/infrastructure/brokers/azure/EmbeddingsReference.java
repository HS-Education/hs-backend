package com.hs.hstesis.repo.infrastructure.brokers.azure;

public record EmbeddingsReference(int schemaVersion, long documentId, int generation, String objectKey,
                                   String sha256, long sizeBytes, int chunkCount, int dimensions) {
    public EmbeddingsReference {
        if (schemaVersion != 1 || documentId < 1 || generation < 1 || sizeBytes < 1
                || sizeBytes > 64L * 1024 * 1024 || chunkCount < 1 || chunkCount > 1500 || dimensions != 1024
                || sha256 == null || sha256.length() != 64 || !sha256.chars().allMatch(c ->
                (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))
                || !("processing-results/" + documentId + "/" + generation + "/" + sha256 + ".json").equals(objectKey)) {
            throw new IllegalArgumentException("Invalid embeddings reference");
        }
    }
}
