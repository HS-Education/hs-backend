package com.hs.hstesis.learning.domain.model.queries;

public record GetAcademicYearByIdQuery(Long id) {
    public GetAcademicYearByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("Academic Year ID cannot be null");
        }
    }
}
