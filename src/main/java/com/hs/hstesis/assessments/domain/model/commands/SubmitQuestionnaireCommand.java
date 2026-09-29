package com.hs.hstesis.assessments.domain.model.commands;

import java.util.Map;

public record SubmitQuestionnaireCommand(
        Long questionnaireInstanceId,
        Map<Long, Integer> answers,
        Long actorId
) {
    public SubmitQuestionnaireCommand {
        if (questionnaireInstanceId == null || questionnaireInstanceId <= 0 || actorId == null || actorId <= 0) {
            throw new IllegalArgumentException("A valid questionnaire instance and student are required.");
        }
        if (answers == null) {
            throw new IllegalArgumentException("Answers are required.");
        }
        if (answers.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
            throw new IllegalArgumentException("Answers must have question IDs and option indexes.");
        }
        answers = Map.copyOf(answers);
    }
}
