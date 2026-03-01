package com.hs.hstesis.learning.domain.model.commands;

public record CreateAcademicLevelCommand(String name) {
    public CreateAcademicLevelCommand {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Academic Level name cannot be null or empty");
        }
    }
}
