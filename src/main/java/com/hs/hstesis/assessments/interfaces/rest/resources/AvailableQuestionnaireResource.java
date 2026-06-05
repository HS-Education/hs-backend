package com.hs.hstesis.assessments.interfaces.rest.resources;

public record AvailableQuestionnaireResource(
        Long id,
        Long courseId,
        Long gradingPeriodId,
        Integer weekNumber,
        String status, // PENDING, STARTED, COMPLETED
        Long activeInstanceId,
        Integer attemptsLeft,
        Integer maxAttempts
) {
}
