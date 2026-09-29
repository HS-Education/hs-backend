package com.hs.hstesis.achievements.domain.model.valueobjects;

import java.util.List;

public record StudentInsightData(String studentCode, Double averageScore, List<TopicPerformance> topics) {}
