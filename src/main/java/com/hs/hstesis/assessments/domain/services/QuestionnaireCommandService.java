package com.hs.hstesis.assessments.domain.services;

import com.hs.hstesis.assessments.domain.model.commands.GenerateQuestionnaireCommand;
import com.hs.hstesis.assessments.domain.model.commands.SubmitQuestionnaireCommand;

public interface QuestionnaireCommandService {
    void handle(GenerateQuestionnaireCommand command);
    void handle(SubmitQuestionnaireCommand command);
    Long handle(com.hs.hstesis.assessments.domain.model.commands.StartQuestionnaireCommand command);
}
