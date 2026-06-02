package com.hs.hstesis.assessments.interfaces.rest.transform;

import com.hs.hstesis.assessments.domain.model.commands.SubmitQuestionnaireCommand;
import com.hs.hstesis.assessments.interfaces.rest.resources.SubmitQuestionnaireResource;

public class SubmitQuestionnaireCommandFromResourceAssembler {
    public static SubmitQuestionnaireCommand toCommandFromResource(Long instanceId, SubmitQuestionnaireResource resource) {
        return new SubmitQuestionnaireCommand(instanceId, resource.answers());
    }
}
