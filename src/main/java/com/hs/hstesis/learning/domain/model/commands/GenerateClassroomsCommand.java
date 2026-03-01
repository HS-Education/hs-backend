package com.hs.hstesis.learning.domain.model.commands;

import java.util.List;

public record GenerateClassroomsCommand(Long academicYearId, Long academicLevelId, List<Long> courseIds) {
    public GenerateClassroomsCommand {
        if (academicYearId == null) {
            throw new IllegalArgumentException("Academic Year ID cannot be null");
        }
        if (academicLevelId == null) {
            throw new IllegalArgumentException("Academic Level ID cannot be null");
        }
        if (courseIds == null || courseIds.isEmpty()) {
            throw new IllegalArgumentException("Course IDs cannot be null or empty");
        }
    }
}
