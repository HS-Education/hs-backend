package com.hs.hstesis.assessments.interfaces.rest.resources;

public record AvailableQuestionnaireResource(
        Long questionnaireId,
        Long courseId,
        Long gradingPeriodId,
        Integer weekNumber,
        String status,
        Long instanceId
) {
}
