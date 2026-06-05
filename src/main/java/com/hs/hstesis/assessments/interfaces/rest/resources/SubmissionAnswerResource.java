package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.util.List;

public record SubmissionAnswerResource(
        Long questionId,
        String questionText,
        List<String> options,
        Integer selectedOptionIndex,
        Integer correctOptionIndex,
        Boolean isCorrect,
        String aiFeedback
) {
}
