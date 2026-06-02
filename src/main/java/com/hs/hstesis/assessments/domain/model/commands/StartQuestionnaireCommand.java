package com.hs.hstesis.assessments.domain.model.commands;

public record StartQuestionnaireCommand(Long questionnaireId, Long studentId) {
}
