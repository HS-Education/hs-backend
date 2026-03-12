package com.hs.hstesis.learning.interfaces.rest.resources;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

public record StudyPlanResource(Long id,
                                EducationLevel educationLevel,
                                GradeLevel gradeLevel,
                                Long courseId,
                                String courseName) {
}
