package com.hs.hstesis.assessments.domain.services;

import com.hs.hstesis.assessments.domain.model.entities.Question;
import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import com.hs.hstesis.assessments.domain.model.queries.GetQuestionnaireInstancesByStudentIdQuery;
import com.hs.hstesis.assessments.domain.model.queries.GetQuestionsByQuestionnaireInstanceIdQuery;

import java.util.List;

public interface QuestionnaireQueryService {
    List<QuestionnaireInstance> handle(GetQuestionnaireInstancesByStudentIdQuery query);
    List<Question> handle(GetQuestionsByQuestionnaireInstanceIdQuery query);
    List<com.hs.hstesis.assessments.interfaces.rest.resources.AvailableQuestionnaireResource> handle(com.hs.hstesis.assessments.domain.model.queries.GetAvailableQuestionnairesQuery query);
    java.util.Optional<com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission> handle(com.hs.hstesis.assessments.domain.model.queries.GetSubmissionByInstanceIdQuery query);
    List<com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission> handle(com.hs.hstesis.assessments.domain.model.queries.GetAllQuestionnaireSubmissionsQuery query);
}
