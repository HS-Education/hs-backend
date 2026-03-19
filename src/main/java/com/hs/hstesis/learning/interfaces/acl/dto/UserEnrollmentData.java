package com.hs.hstesis.learning.interfaces.acl.dto;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

public record UserEnrollmentData(EducationLevel level,
                                 GradeLevel grade,
                                 Long courseId) {}
