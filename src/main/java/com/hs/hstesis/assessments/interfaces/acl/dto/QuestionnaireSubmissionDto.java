package com.hs.hstesis.assessments.interfaces.acl.dto;

import java.time.LocalDateTime;

public record QuestionnaireSubmissionDto(
        Long submissionId,
        Long questionnaireId,
        Long studentId,
        Integer score,
        LocalDateTime submittedAt
) {
}
