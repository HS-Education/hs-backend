package com.hs.hstesis.learning.domain.model.commands;

public record UpdateCourseCommand(Long id, String name) {
    public UpdateCourseCommand {
        if (id == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be blank");
        }
    }
}
