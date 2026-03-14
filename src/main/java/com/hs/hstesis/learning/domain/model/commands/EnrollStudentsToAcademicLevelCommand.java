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
        if (studentIds == null || studentIds.isEmpty() || studentIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Student ids cannot be null, empty or negative.");
        }
        if (educationLevel == null) {
            throw new IllegalArgumentException("Education level cannot be .");
        }
        if (gradeLevel == null) {
            throw new IllegalArgumentException("Grade level cannot be null.");
        }
        if (academicYearId == null || academicYearId <= 0) {
            throw new IllegalArgumentException("Academic year id cannot be null or negative.");
        }
    }
}
