package com.hs.hstesis.achievements.domain.model.valueobjects;

public record TopicPerformance(
        Long topicId,
        String topicName,
        Integer weekNumber,
        Integer score,
        Double percentage
) {}
