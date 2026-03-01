package com.hs.hstesis.learning.domain.model.commands;

public record ActivateAcademicYearCommand(Long id) {
    public ActivateAcademicYearCommand {
        if(id == null){
            throw new IllegalArgumentException("Academic Year ID cannot be null");
        }
    }
}
