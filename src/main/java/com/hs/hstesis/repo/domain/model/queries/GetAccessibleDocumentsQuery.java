package com.hs.hstesis.repo.domain.model.queries;

public record GetAccessibleDocumentsQuery(Long courseId) {
    public GetAccessibleDocumentsQuery {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
    }
}
