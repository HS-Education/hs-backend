package com.hs.hstesis.learning.domain.model.commands;

public record EditAcademicLevelNameCommand(Long id, String newName) {
    public EditAcademicLevelNameCommand {
        if (id == null) {
            throw new IllegalArgumentException("Academic Level ID cannot be null");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("New name cannot be null or empty");
        }
    }
}
