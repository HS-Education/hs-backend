package com.hs.hstesis.learning.domain.model.queries;

public record GetStudyPlanByIdQuery(Long studyPlanId) {
    public GetStudyPlanByIdQuery {
        if (studyPlanId == null || studyPlanId <= 0) {
            throw new IllegalArgumentException("Study plan id cannot be null or negative.");
        }
    }
}
