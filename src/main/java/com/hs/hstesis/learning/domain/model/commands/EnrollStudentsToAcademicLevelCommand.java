package com.hs.hstesis.learning.domain.model.commands;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

import java.util.List;

public record EnrollStudentsToAcademicLevelCommand(
        List<Long> studentIds,
        EducationLevel educationLevel,
        GradeLevel gradeLevel,
        Long academicYearId
) {
    public EnrollStudentsToAcademicLevelCommand {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new IllegalArgumentException("Student id's cannot be null or empty");
        }
        if (educationLevel == null) {
            throw new IllegalArgumentException("Education level cannot be null");
        }
        if (gradeLevel == null) {
            throw new IllegalArgumentException("Grade level cannot be null");
        }
        if (academicYearId == null || academicYearId <= 0) {
            throw new IllegalArgumentException("Academic Year id cannot be null or negative");
        }
    }
}
