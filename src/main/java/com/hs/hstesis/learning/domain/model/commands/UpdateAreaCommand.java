package com.hs.hstesis.learning.domain.model.commands;

public record UpdateAreaCommand(Long id, String name) {
    public UpdateAreaCommand {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Area name cannot be blank");
        }
    }
}
