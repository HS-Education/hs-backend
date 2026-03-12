package com.hs.hstesis.learning.domain.model.commands;

public record CreateCourseCommand(String name, Long areaId) {
    public CreateCourseCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be null or blank");
        }
        if (areaId == null || areaId <= 0) {
            throw new IllegalArgumentException("Area id cannot be null or negative");
        }
    }
}
