package com.hs.hstesis.repo.domain.model.valueobjects;

public record UserEnrollmentContext(
        EducationLevel educationLevel,
        GradeLevel gradeLevel,
        Long courseId
) {}
