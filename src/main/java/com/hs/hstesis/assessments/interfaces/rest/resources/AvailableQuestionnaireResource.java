package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.util.List;

public record AvailableQuestionnaireResource(
        Long id,
        Long courseId,
        Long gradingPeriodId,
        Integer weekNumber,
        String status, // PENDING, STARTED, COMPLETED
        Long activeInstanceId,
        Integer attemptsLeft,
        Integer maxAttempts,
        Integer questionsPerAttempt,
        String type,
        List<QuestionnaireAttemptResource> pastAttempts,
        String createdAt,
        boolean informationalOnly
) {
}
