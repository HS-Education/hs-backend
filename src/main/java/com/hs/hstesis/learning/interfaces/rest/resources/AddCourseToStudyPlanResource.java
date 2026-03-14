package com.hs.hstesis.learning.interfaces.rest.resources;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

public record AddCourseToStudyPlanResource(Long courseId,
                                           EducationLevel educationLevel,
                                           GradeLevel gradeLevel
){}

