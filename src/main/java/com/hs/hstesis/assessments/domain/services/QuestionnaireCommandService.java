package com.hs.hstesis.assessments.domain.services;

import com.hs.hstesis.assessments.domain.model.commands.GenerateQuestionnaireCommand;
import com.hs.hstesis.assessments.domain.model.commands.SubmitQuestionnaireCommand;

public interface QuestionnaireCommandService {
    void handle(GenerateQuestionnaireCommand command);
    void handle(com.hs.hstesis.assessments.domain.model.commands.GenerateRemedialQuestionnaireCommand command);
    com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission handle(SubmitQuestionnaireCommand command);
    void retryFeedback(com.hs.hstesis.assessments.domain.model.commands.RetryQuestionnaireFeedbackCommand command);
    Long handle(com.hs.hstesis.assessments.domain.model.commands.StartQuestionnaireCommand command);
}
