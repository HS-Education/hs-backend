package com.hs.hstesis.assessments.domain.model.commands;

import java.util.Map;

public record SubmitQuestionnaireCommand(
        Long questionnaireInstanceId,
        Map<Long, Integer> answers
) {
}
