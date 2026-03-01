package com.hs.hstesis.learning.domain.model.queries;

public record GetCourseByIdQuery(Long id) {
    public GetCourseByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}