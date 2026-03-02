package com.hs.hstesis.learning.domain.model.queries;

public record GetAreaCoordinatorByIdQuery(Long id) {
    public GetAreaCoordinatorByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("Area Coordinator ID cannot be null");
        }
    }
}
