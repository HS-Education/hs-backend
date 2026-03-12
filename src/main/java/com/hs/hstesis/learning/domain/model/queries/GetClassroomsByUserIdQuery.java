package com.hs.hstesis.learning.domain.model.queries;

public record GetClassroomsByUserIdQuery(Long userId) {
    public GetClassroomsByUserIdQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id cannot be null or negative");
        }
    }
}
