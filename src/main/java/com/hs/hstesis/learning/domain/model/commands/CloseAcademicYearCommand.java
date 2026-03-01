package com.hs.hstesis.learning.domain.model.commands;

public record CloseAcademicYearCommand(Long id) {
    public CloseAcademicYearCommand {
        if (id == null) {
            throw new IllegalArgumentException("Academic year ID cannot be null");
        }
    }
}
