package com.hs.hstesis.learning.domain.model.commands;

public record AddTopicCommand(Long courseId, String name, Integer orderIndex) {
    public AddTopicCommand {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        if (orderIndex == null || orderIndex <= 0) {
            throw new IllegalArgumentException("Order index cannot be null or negative.");
        }
    }
}
