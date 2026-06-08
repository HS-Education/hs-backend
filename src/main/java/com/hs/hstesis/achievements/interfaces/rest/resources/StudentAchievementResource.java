package com.hs.hstesis.achievements.interfaces.rest.resources;

import com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformance;

public record StudentAchievementResource(
        StudentPerformance performance,
        String latestInsight
) {}
