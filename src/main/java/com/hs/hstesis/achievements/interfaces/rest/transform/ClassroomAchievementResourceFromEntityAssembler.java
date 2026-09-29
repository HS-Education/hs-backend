package com.hs.hstesis.achievements.interfaces.rest.transform;

import com.hs.hstesis.achievements.domain.model.valueobjects.ClassroomPerformance;
import com.hs.hstesis.achievements.interfaces.rest.resources.ClassroomAchievementResource;

public class ClassroomAchievementResourceFromEntityAssembler {
    public static ClassroomAchievementResource toResourceFromEntity(ClassroomPerformance performance, String latestInsight) {
        return new ClassroomAchievementResource(performance, latestInsight, null);
    }
}
