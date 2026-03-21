package com.hs.hstesis.repo.domain.model.queries;

public record GetDocumentByIdQuery(Long documentId) {
    public GetDocumentByIdQuery {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document id cannot be null or negative");
        }
    }
}
