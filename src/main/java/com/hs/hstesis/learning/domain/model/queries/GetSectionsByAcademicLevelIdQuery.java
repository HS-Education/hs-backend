package com.hs.hstesis.learning.domain.model.queries;

public record GetSectionsByAcademicLevelIdQuery(Long academicLevelId) {
    public GetSectionsByAcademicLevelIdQuery {
        if (academicLevelId == null) {
            throw new IllegalArgumentException("Academic level id cannot be null");
        }
    }
}
