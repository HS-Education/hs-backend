package com.hs.hstesis.learning.domain.model.commands;

public record CreateAreaCommand (String name){
    public CreateAreaCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Area name cannot be null or blank");
        }
    }
}
