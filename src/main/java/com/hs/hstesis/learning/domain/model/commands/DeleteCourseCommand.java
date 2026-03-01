package com.hs.hstesis.learning.domain.model.commands;

public record DeleteCourseCommand(Long id) {
    public DeleteCourseCommand {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
