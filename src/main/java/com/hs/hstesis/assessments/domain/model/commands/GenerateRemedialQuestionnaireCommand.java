package com.hs.hstesis.assessments.domain.model.commands;

public record GenerateRemedialQuestionnaireCommand(
        Long studentId,
        Long courseId,
        Long gradingPeriodId,
        Integer weekNumber,
        Long topicId
) {}
