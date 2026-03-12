package com.hs.hstesis.learning.domain.model.commands;

public record DeleteSectionCommand(Long id) {
    public DeleteSectionCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Section id cannot be null or negative");
        }
    }
}
