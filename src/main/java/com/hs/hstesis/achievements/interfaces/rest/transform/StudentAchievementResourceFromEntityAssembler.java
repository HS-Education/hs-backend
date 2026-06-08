package com.hs.hstesis.achievements.interfaces.rest.transform;

import com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformance;
import com.hs.hstesis.achievements.interfaces.rest.resources.StudentAchievementResource;

public class StudentAchievementResourceFromEntityAssembler {
    public static StudentAchievementResource toResourceFromEntity(StudentPerformance performance, String latestInsight) {
        return new StudentAchievementResource(performance, latestInsight);
    }
}
