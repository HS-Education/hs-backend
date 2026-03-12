package com.hs.hstesis.learning.domain.model.queries;

public record GetCoursesByAreaIdQuery(Long areaId) {
    public GetCoursesByAreaIdQuery {
        if (areaId == null || areaId <= 0) {
            throw new IllegalArgumentException("Area id cannot be null or negative");
        }
    }
}
