package com.hs.hstesis.learning.domain.model.commands;

import java.util.List;

public record AssignTeacherToClassroomsCommand(Long teacherId, List<Long> classroomIds) {
    public AssignTeacherToClassroomsCommand {
        if (teacherId == null) {
            throw new IllegalArgumentException("Teacher ID cannot be null");
        }
        if (classroomIds == null) {
            throw new IllegalArgumentException("Classroom IDs cannot be null");
        }
    }
}
