package com.hs.hstesis.assessments.domain.model.commands;

public record RetryQuestionnaireFeedbackCommand(Long questionnaireInstanceId, Long studentId) {}
