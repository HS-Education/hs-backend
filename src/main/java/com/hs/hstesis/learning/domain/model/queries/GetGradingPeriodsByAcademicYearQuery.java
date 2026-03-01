package com.hs.hstesis.learning.domain.model.queries;

public record GetGradingPeriodsByAcademicYearQuery(Long academicYearId) {
    public GetGradingPeriodsByAcademicYearQuery {
        if (academicYearId == null) {
            throw new IllegalArgumentException("Academic Year ID cannot be null");
        }
    }
}