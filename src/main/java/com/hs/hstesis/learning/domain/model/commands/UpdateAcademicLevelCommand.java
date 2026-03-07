package com.hs.hstesis.learning.domain.model.commands;

public record UpdateAcademicLevelCommand(Long id, String name) {
    public UpdateAcademicLevelCommand {
        if (id == null) {
            throw new IllegalArgumentException("Academic Level ID cannot be null");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Academic Level name cannot be blank");
        }
    }
}
