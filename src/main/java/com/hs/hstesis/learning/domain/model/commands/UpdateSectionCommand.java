package com.hs.hstesis.learning.domain.model.commands;

public record UpdateSectionCommand(Long id, String name){
    public UpdateSectionCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Section id cannot be null or negative");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Section name cannot be blank");
        }
    }
}
