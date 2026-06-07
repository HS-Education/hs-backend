package com.hs.hstesis.assessments.domain.model.commands;

import java.util.List;

public record CreateDirectQuestionnaireCommand(
        Long courseId,
        Long gradingPeriodId,
        Integer weekNumber,
        Long topicId,
        Integer allowedAttempts,
        Integer questionsPerAttempt,
        List<QuestionDraftRecord> questions
) {
}
