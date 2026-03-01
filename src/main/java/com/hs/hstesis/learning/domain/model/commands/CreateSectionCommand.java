package com.hs.hstesis.learning.domain.model.commands;

public record CreateSectionCommand(String name, Long academicLevelId) {
    public CreateSectionCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Section name cannot be null or blank");
        }
        if (academicLevelId == null) {
            throw new IllegalArgumentException("Academic Level ID cannot be null");
        }
    }
}
