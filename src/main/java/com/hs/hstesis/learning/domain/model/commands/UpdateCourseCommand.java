package com.hs.hstesis.learning.domain.model.commands;

public record UpdateCourseCommand(Long id, String name) {
    public UpdateCourseCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be blank");
        }
    }
}
