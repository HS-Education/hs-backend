package com.hs.hstesis.learning.domain.model.queries;

public record GetAreaCoordinatorByAreaIdQuery(Long areaId) {
    public GetAreaCoordinatorByAreaIdQuery {
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }
    }
}
