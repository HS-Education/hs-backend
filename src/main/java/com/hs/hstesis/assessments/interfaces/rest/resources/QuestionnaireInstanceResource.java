package com.hs.hstesis.assessments.interfaces.rest.resources;

public record QuestionnaireInstanceResource(
        Long id,
        Long questionnaireId,
        Long studentId
) {
}
