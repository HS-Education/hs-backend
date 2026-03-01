package com.hs.hstesis.learning.domain.model.commands;

public record CreateAcademicYearCommand(Integer year) {
    public CreateAcademicYearCommand {
        if (year == null) {
            throw new IllegalArgumentException("Year cannot be null");
        }
    }
}
