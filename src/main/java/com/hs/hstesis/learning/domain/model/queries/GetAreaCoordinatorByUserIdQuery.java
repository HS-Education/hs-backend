package com.hs.hstesis.learning.domain.model.queries;

public record GetAreaCoordinatorByUserIdQuery(Long userId) {
    public GetAreaCoordinatorByUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }
}
