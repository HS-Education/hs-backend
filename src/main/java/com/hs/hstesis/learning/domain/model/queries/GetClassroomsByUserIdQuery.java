package com.hs.hstesis.learning.domain.model.queries;

public record GetClassroomsByUserIdQuery(Long userId) {
    public GetClassroomsByUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }
}
