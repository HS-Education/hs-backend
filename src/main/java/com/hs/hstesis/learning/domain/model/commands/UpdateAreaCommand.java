package com.hs.hstesis.learning.domain.model.commands;

public record UpdateAreaCommand(Long id, String name, Long coordinatorId) {
    public UpdateAreaCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Area id cannot be null or negative");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Area name cannot be blank");
        }
        if (coordinatorId != null && coordinatorId <= 0) {
            throw new IllegalArgumentException("Coordinator id cannot be null or negative");
        }
    }
}
