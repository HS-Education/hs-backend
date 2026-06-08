package com.hs.hstesis.achievements.interfaces.rest.transform;

import com.hs.hstesis.achievements.domain.model.valueobjects.AreaPerformance;
import com.hs.hstesis.achievements.interfaces.rest.resources.AreaAchievementResource;

public class AreaAchievementResourceFromEntityAssembler {
    public static AreaAchievementResource toResourceFromEntity(AreaPerformance performance, String latestInsight) {
        return new AreaAchievementResource(performance, latestInsight);
    }
}
