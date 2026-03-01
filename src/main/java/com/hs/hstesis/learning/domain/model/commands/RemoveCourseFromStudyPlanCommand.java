package com.hs.hstesis.learning.domain.model.commands;

public record RemoveCourseFromStudyPlanCommand(Long studyPlanId) {
    public RemoveCourseFromStudyPlanCommand {
        if (studyPlanId == null) {
            throw new IllegalArgumentException("Study Plan ID cannot be null");
        }
    }
}
