package com.hs.hstesis.learning.domain.model.commands;

public record DeleteCourseCommand(Long id) {
    public DeleteCourseCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative");
        }
    }
}
