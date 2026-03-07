package com.hs.hstesis.learning.domain.model.commands;

public record UpdateSectionCommand(Long id, String name){
    public UpdateSectionCommand {
        if (id == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Section name cannot be blank");
        }
    }
}
