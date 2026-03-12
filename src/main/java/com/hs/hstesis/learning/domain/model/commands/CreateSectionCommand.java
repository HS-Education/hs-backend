package com.hs.hstesis.learning.domain.model.commands;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

public record CreateSectionCommand(String name, EducationLevel educationLevel, GradeLevel gradeLevel) {
    public CreateSectionCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Section name cannot be null or blank");
        }
        if (educationLevel == null) {
            throw new IllegalArgumentException("Education Level cannot be null");
        }
        if (gradeLevel == null) {
            throw new IllegalArgumentException("Grade Level cannot be null");
        }
    }
}
