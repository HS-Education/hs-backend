package com.hs.hstesis.learning.domain.model.commands;

import java.util.List;

public record UnenrollStudentFromClassroomsCommand(Long studentId, List<Long> classroomIds) {
    public UnenrollStudentFromClassroomsCommand {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("Student id cannot be null or negative.");
        }
        if (classroomIds == null || classroomIds.isEmpty() || classroomIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Classroom ids cannot be null, empty or negative.");
        }
    }
}
