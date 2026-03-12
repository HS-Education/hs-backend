package com.hs.hstesis.learning.domain.model.commands;

public record DeleteClassroomCommand(Long id) {
    public DeleteClassroomCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Classroom id cannot be null or negative.");
        }
    }
}
