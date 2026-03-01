package com.hs.hstesis.learning.domain.model.commands;

public record DeleteSectionCommand(Long id) {
    public DeleteSectionCommand {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
