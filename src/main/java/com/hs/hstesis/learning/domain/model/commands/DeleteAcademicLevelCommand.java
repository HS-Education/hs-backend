package com.hs.hstesis.learning.domain.model.commands;

public record DeleteAcademicLevelCommand(Long id) {
    public DeleteAcademicLevelCommand {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
