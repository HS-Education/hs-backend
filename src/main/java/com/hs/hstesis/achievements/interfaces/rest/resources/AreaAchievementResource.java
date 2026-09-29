package com.hs.hstesis.achievements.interfaces.rest.resources;

import com.hs.hstesis.achievements.domain.model.valueobjects.AreaPerformance;

public record AreaAchievementResource(
        AreaPerformance performance,
        String latestInsight,
        java.util.Date latestInsightCreatedAt
) {}
