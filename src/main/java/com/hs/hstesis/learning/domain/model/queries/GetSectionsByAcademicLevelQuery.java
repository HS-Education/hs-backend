package com.hs.hstesis.learning.domain.model.queries;

public record GetSectionsByAcademicLevelQuery(Long academicLevelId) {
    public GetSectionsByAcademicLevelQuery {
        if (academicLevelId == null) {
            throw new IllegalArgumentException("Academic level id cannot be null");
        }
    }
}
