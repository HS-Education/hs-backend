package com.hs.hstesis.assessments.domain.model.commands;

public record GenerateQuestionnaireCommand(
        Long courseId,
        Long gradingPeriodId,
        Integer weekNumber,
        Integer allowedAttempts,
        Integer questionsPerAttempt
) {
}
