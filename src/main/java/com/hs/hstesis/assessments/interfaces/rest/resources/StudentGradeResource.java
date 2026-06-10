package com.hs.hstesis.assessments.interfaces.rest.resources;

import java.time.LocalDateTime;

public record StudentGradeResource(
        Long studentId,
        Long questionnaireInstanceId,
        Long questionnaireId,
        Long courseId,
        Integer weekNumber,
        Integer score,
        LocalDateTime submittedAt
) {
}
