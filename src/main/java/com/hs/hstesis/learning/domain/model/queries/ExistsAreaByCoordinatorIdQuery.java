package com.hs.hstesis.learning.domain.model.queries;

public record ExistsAreaByCoordinatorIdQuery(Long coordinatorId) {
    public ExistsAreaByCoordinatorIdQuery {
        if (coordinatorId == null || coordinatorId <= 0) {
            throw new IllegalArgumentException("Coordinator id cannot be null or negative.");
        }
    }
}
