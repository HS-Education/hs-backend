package com.hs.hstesis.learning.domain.model.queries;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

public record GetStudyPlanByEducationLevelAndGradeLevelQuery(EducationLevel educationLevel, GradeLevel gradeLevel) {
    public GetStudyPlanByEducationLevelAndGradeLevelQuery {
        if (educationLevel == null) {
            throw new IllegalArgumentException("Academic level cannot be null");
        }
        if (gradeLevel == null) {
            throw new IllegalArgumentException("Grade level cannot be null");
        }
    }
}
