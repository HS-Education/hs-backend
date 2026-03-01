package com.hs.hstesis.learning.domain.model.queries;

public record GetClassroomsByAreaIdQuery(Long areaId) {
    public GetClassroomsByAreaIdQuery {
        if (areaId == null) {
            throw new IllegalArgumentException("AreaId cannot be null");
        }
    }
}
