package com.hs.hstesis.repo.domain.model.queries;

public record GetDocumentsByCoordinatorQuery(Long userId) {
    public GetDocumentsByCoordinatorQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalStateException("Coordinator id cannot be null or negative.");
        }
    }
}
