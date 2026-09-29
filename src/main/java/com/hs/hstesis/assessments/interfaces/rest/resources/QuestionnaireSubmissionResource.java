package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.util.List;

public record QuestionnaireSubmissionResource(
        Integer score,
        com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus feedbackStatus,
        List<SubmissionAnswerResource> answers
) {
}
