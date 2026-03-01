package com.hs.hstesis.learning.domain.model.commands;

public record AddCourseToStudyPlanCommand(Long academicLevelId, Long courseId) {
    public AddCourseToStudyPlanCommand {
        if (academicLevelId == null) {
            throw new IllegalArgumentException("Academic level ID cannot be null");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
    }
}
