package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.time.LocalDateTime;

public record QuestionnaireAttemptResource(
        Long instanceId,
        Integer score,
        LocalDateTime submittedAt,
        Integer totalQuestions
) {
    public QuestionnaireAttemptResource(Long instanceId, Integer score, LocalDateTime submittedAt) {
        this(instanceId, score, submittedAt, 5);
    }
}
