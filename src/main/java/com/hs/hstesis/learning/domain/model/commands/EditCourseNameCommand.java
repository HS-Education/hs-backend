package com.hs.hstesis.learning.domain.model.commands;

public record EditCourseNameCommand(Long id, String newName) {
    public EditCourseNameCommand {
        if (id == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("New course name cannot be null or empty");
        }
    }
}
