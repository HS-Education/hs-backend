package com.hs.hstesis.learning.domain.model.commands;

public record DeleteClassroomCommand(Long id) {
    public DeleteClassroomCommand {
        if (id == null) {
            throw new IllegalArgumentException("Classroom ID cannot be null");
        }
    }
}
