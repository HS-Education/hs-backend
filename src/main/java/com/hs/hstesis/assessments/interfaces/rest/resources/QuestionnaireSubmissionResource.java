package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.util.List;

public record QuestionnaireSubmissionResource(
        Integer score,
        List<SubmissionAnswerResource> answers
) {
}
