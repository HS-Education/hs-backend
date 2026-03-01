package com.hs.hstesis.learning.domain.model.commands;

public record DeleteAcademicYearCommand(Long id) {
    public DeleteAcademicYearCommand {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
