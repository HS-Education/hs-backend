package com.hs.hstesis.learning.domain.model.commands;

import java.util.List;

public record AssignTeacherToClassroomsCommand(Long teacherId, List<Long> classroomIds) {
    public AssignTeacherToClassroomsCommand {
        if (teacherId == null || teacherId <= 0) {
            throw new IllegalArgumentException("Teacher id cannot be null or negative.");
        }
        if (classroomIds == null || classroomIds.isEmpty() ||  classroomIds.stream().anyMatch(id -> id <= 0)) {
            throw new IllegalArgumentException("Classroom ids cannot be null, empty or negative.");
        }
    }
}
