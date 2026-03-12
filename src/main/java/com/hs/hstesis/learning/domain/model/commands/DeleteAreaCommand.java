package com.hs.hstesis.learning.domain.model.commands;

public record DeleteAreaCommand(Long id) {
    public DeleteAreaCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Area id cannot be null or negative");
        }
    }
}