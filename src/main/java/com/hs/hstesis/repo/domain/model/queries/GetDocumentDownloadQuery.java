package com.hs.hstesis.repo.domain.model.queries;

public record GetDocumentDownloadQuery(Long documentId, Long courseId) {
    public GetDocumentDownloadQuery {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document id cannot be null or negative.");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
    }
}
