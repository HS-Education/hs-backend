package com.hs.hstesis.learning.domain.model.queries;

public record GetAcademicLevelByIdQuery(Long id) {
    public GetAcademicLevelByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}