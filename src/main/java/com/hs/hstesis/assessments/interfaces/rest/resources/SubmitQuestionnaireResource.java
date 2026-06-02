package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.util.Map;

public record SubmitQuestionnaireResource(
        Map<Long, Integer> answers
) {
}
