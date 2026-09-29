package com.hs.hstesis.achievements.interfaces.rest.resources;

import com.hs.hstesis.achievements.domain.model.valueobjects.ClassroomPerformance;

public record ClassroomAchievementResource(
        ClassroomPerformance performance,
        String latestInsight,
        java.util.Date latestInsightCreatedAt
) {}
