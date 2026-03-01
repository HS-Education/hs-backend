package com.hs.hstesis.learning.domain.model.commands;

public record UnenrollUserCommand(Long userId, Long classroomId) {
    public UnenrollUserCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        if (classroomId == null) {
            throw new IllegalArgumentException("Classroom ID cannot be null.");
        }
    }
}
