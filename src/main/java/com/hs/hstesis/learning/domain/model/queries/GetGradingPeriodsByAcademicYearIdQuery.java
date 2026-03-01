package com.hs.hstesis.learning.domain.model.queries;

public record GetGradingPeriodsByAcademicYearIdQuery(Long academicYearId) {
    public GetGradingPeriodsByAcademicYearIdQuery {
        if (academicYearId == null) {
            throw new IllegalArgumentException("Academic Year ID cannot be null");
        }
    }
}