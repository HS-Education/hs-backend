package com.hs.hstesis.learning.domain.model.queries;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

public record GetSectionsByEducationAndGradeLevelQuery(
        EducationLevel educationLevel,
        GradeLevel gradeLevel
) {
    public GetSectionsByEducationAndGradeLevelQuery {
        if (educationLevel == null) {
            throw new IllegalArgumentException("EducationLevel cannot be null");
        }
        if (gradeLevel == null) {
            throw new IllegalArgumentException("GradeLevel cannot be null");
        }
    }
}
