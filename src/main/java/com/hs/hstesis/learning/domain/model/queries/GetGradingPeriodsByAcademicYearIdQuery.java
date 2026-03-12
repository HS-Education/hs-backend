package com.hs.hstesis.learning.domain.model.queries;

public record GetGradingPeriodsByAcademicYearIdQuery(Long academicYearId) {
    public GetGradingPeriodsByAcademicYearIdQuery {
        if (academicYearId == null || academicYearId <= 0) {
            throw new IllegalArgumentException("Academic year id cannot be null or negative.");
        }
    }
}