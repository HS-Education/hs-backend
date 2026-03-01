package com.hs.hstesis.learning.domain.model.commands;

public record EditAreaNameCommand(Long id, String newName) {
    public EditAreaNameCommand {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("New name cannot be null or blank");
        }
    }
}
