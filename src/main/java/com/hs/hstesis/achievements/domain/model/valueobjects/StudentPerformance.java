package com.hs.hstesis.achievements.domain.model.valueobjects;

import java.util.List;

public record StudentPerformance(
        Long studentId,
        String studentName,
        Double averageScore,
        List<TopicPerformance> topics
) {}
