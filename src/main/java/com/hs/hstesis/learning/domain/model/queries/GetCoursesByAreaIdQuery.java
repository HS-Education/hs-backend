package com.hs.hstesis.learning.domain.model.queries;

public record GetCoursesByAreaIdQuery(Long areaId) {
    public GetCoursesByAreaIdQuery {
        if (areaId == null) {
            throw new IllegalArgumentException("areaId cannot be null");
        }
    }
}
