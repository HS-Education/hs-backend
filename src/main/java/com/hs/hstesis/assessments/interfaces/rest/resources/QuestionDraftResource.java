package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.util.List;

public record QuestionDraftResource(
        String text,
        List<String> options,
        Integer correctOptionIndex
) {
}
