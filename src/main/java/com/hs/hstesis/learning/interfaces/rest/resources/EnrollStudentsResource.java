package com.hs.hstesis.learning.interfaces.rest.resources;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

import java.util.List;

public record EnrollStudentsResource(List<Long> studentIds,
                                     EducationLevel  educationLevel,
                                     GradeLevel gradeLevel,
                                     Long academicYearId) { }
