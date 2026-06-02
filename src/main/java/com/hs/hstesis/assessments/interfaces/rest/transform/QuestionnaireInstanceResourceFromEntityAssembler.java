package com.hs.hstesis.assessments.interfaces.rest.transform;

import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import com.hs.hstesis.assessments.interfaces.rest.resources.QuestionnaireInstanceResource;

public class QuestionnaireInstanceResourceFromEntityAssembler {
    public static QuestionnaireInstanceResource toResourceFromEntity(QuestionnaireInstance entity) {
        return new QuestionnaireInstanceResource(
                entity.getId(),
                entity.getQuestionnaire().getId(),
                entity.getStudentId()
        );
    }
}
