package com.hs.hstesis.learning.domain.model.queries;

public record GetClassroomByIdQuery(Long id) {
    public GetClassroomByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("Classroom ID cannot be null");
        }
    }
}
