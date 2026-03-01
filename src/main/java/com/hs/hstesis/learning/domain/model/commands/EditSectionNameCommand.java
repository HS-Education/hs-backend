package com.hs.hstesis.learning.domain.model.commands;

public record EditSectionNameCommand (Long id, String newName){
    public EditSectionNameCommand {
        if (id == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Section name cannot be null or empty");
        }
    }
}
