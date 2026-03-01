package com.hs.hstesis.learning.domain.model.commands;

public record DeleteAreaCommand(Long id) {
    public DeleteAreaCommand {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}