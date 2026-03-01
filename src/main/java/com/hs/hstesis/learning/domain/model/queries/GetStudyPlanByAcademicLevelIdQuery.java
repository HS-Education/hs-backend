package com.hs.hstesis.learning.domain.model.queries;

public record GetStudyPlanByAcademicLevelIdQuery(Long academicLevelId) {
    public GetStudyPlanByAcademicLevelIdQuery {
        if (academicLevelId == null) {
            throw new IllegalArgumentException("Academic level ID cannot be null");
        }
    }
}
