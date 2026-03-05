package com.hs.hstesis.learning.domain.model.queries;

public record GetStudyPlanByAcademicLevelIdAndCourseIdQuery(Long academicLevelId, Long courseId) {
    public GetStudyPlanByAcademicLevelIdAndCourseIdQuery {
        if (academicLevelId == null) {
            throw new IllegalArgumentException("Academic level ID cannot be null");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
    }
}
