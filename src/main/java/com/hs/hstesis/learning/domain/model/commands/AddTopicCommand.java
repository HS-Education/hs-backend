package com.hs.hstesis.learning.domain.model.commands;

public record AddTopicCommand(Long courseId, Long gradingPeriodId, String name) {
    public AddTopicCommand {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
    }
}
