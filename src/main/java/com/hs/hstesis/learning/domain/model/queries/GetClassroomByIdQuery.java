package com.hs.hstesis.learning.domain.model.queries;

public record GetClassroomByIdQuery(Long id) {
    public GetClassroomByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Classroom id cannot be null or negative");
        }
    }
}
