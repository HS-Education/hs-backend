package com.hs.hstesis.learning.domain.model.queries;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

public record GetStudyPlanByEducationLevelAndGradeLevelAndCourseIdQuery(EducationLevel educationLevel, GradeLevel gradeLevel, Long courseId) {
    public GetStudyPlanByEducationLevelAndGradeLevelAndCourseIdQuery {
        if (educationLevel == null) {
            throw new IllegalArgumentException("Education level cannot be null");
        }
        if (gradeLevel == null) {
            throw new IllegalArgumentException("Grade level cannot be null");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative");
        }
    }
}
