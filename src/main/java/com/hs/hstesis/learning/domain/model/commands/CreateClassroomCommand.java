package com.hs.hstesis.learning.domain.model.commands;

public record CreateClassroomCommand(Long courseId, Long sectionId, Long academicYearId) {
    public CreateClassroomCommand {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (sectionId == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
        if (academicYearId == null) {
            throw new IllegalArgumentException("Academic Year ID cannot be null");
        }
    }
}