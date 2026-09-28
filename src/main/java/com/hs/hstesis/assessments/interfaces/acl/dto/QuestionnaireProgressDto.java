package com.hs.hstesis.assessments.interfaces.acl.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionnaireProgressDto(
        Long questionnaireId,
        Long gradingPeriodId,
        Integer weekNumber,
        String type,
        String status,
        List<Submission> submissions
) {
    public record Submission(Long studentId, Integer score, LocalDateTime submittedAt, List<Answer> answers) {}

    public record Answer(Long questionId, Long topicId, String questionText, Boolean correct, Boolean remedial) {}
}
