package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.util.List;

public record QuestionResource(
        Long id,
        Long topicId,
        String text,
        List<String> options,
        Boolean isRemedial
) {
}
