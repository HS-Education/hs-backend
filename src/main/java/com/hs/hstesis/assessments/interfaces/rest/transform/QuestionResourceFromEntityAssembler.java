package com.hs.hstesis.assessments.interfaces.rest.transform;

import com.hs.hstesis.assessments.domain.model.entities.Question;
import com.hs.hstesis.assessments.interfaces.rest.resources.QuestionResource;

public class QuestionResourceFromEntityAssembler {
    public static QuestionResource toResourceFromEntity(Question entity) {
        return new QuestionResource(
                entity.getId(),
                entity.getTopicId(),
                entity.getText(),
                entity.getOptions(),
                entity.getIsRemedial()
        );
    }
}
