package com.hs.hstesis.assessments.interfaces.rest.resources;

public record SubmissionAnswerResource(
        Long questionId,
        String questionText,
        Integer selectedOptionIndex,
        Boolean isCorrect,
        String aiFeedback
) {
}
