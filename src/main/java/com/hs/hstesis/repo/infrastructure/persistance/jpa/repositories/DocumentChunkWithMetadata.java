package com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories;

public interface DocumentChunkWithMetadata {
    String getContent();
    String getTitle();
    Long getDocumentId();
    Long getCourseId();
    Double getSimilarity();
}
