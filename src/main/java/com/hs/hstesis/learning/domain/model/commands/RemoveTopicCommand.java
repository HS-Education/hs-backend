package com.hs.hstesis.learning.domain.model.commands;

public record RemoveTopicCommand(Long courseId, Long topicId) {
    public RemoveTopicCommand {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
        if (topicId == null || topicId <= 0) {
            throw new IllegalArgumentException("Topic id cannot be null or negative.");
        }
    }
}
