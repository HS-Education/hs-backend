package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.util.List;

public record CreateDirectQuestionnaireResource(
        Long courseId,
        Long gradingPeriodId,
        Integer weekNumber,
        Long topicId,
        Integer allowedAttempts,
        Integer questionsPerAttempt,
        List<QuestionDraftResource> questions
) {
}
