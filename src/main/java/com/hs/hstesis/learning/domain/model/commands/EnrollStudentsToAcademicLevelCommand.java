package com.hs.hstesis.learning.domain.model.commands;

import java.util.List;

public record EnrollStudentsToAcademicLevelCommand(List<Long> studentIds, Long academicLevelId, Long academicYearId) {
    public EnrollStudentsToAcademicLevelCommand {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new IllegalArgumentException("Student IDs cannot be null or empty");
        }
        if (academicLevelId == null) {
            throw new IllegalArgumentException("Academic Level ID cannot be null");
        }
        if (academicYearId == null) {
            throw new IllegalArgumentException("Academic Year ID cannot be null");
        }
    }
}
