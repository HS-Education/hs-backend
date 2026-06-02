package com.hs.hstesis.learning.domain.model.commands;

import java.util.List;

public record ReorderTopicsCommand(Long courseId, List<TopicOrderDto> topics) {
    public ReorderTopicsCommand {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
        if (topics == null || topics.isEmpty()) {
            throw new IllegalArgumentException("Topics cannot be null or empty.");
        }
    }
}
