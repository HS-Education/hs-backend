package com.hs.hstesis.learning.domain.model.commands;

import java.util.List;

public record ReorderTopicsCommand(Long courseId, List<Long> topicIdsInOrder) {
    public RemoveTopicCommand {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
        if (topicIdsInOrder == null || topicIdsInOrder.isEmpty()) {
            throw new IllegalArgumentException("Topic ids cannot be null or empty.");
        }
    }
}
