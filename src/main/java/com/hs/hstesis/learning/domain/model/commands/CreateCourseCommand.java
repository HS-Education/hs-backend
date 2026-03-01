package com.hs.hstesis.learning.domain.model.commands;

public record CreateCourseCommand(String name, Long areaId) {
    public CreateCourseCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or blank");
        }
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }
    }
}
