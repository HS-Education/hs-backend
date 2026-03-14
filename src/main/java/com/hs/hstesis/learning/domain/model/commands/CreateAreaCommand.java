package com.hs.hstesis.learning.domain.model.commands;

public record CreateAreaCommand (String name, Long coordinatorId) {
    public CreateAreaCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Area name cannot be null or blank");
        }
        if (coordinatorId == null || coordinatorId <= 0) {
            throw new IllegalArgumentException("Coordinator id cannot be null or negative.");
        }
    }
}
