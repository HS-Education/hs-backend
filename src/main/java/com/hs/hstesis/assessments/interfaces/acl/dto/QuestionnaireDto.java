package com.hs.hstesis.assessments.interfaces.acl.dto;

public record QuestionnaireDto(
        Long questionnaireId,
        Long courseId,
        Long gradingPeriodId,
        Integer weekNumber,
        String status
) {
}
