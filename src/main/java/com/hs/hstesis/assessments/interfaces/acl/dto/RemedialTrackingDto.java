package com.hs.hstesis.assessments.interfaces.acl.dto;

public record RemedialTrackingDto(
        Long studentId,
        Long courseId,
        Long weakTopicId,
        Integer lastRemedialScore,
        Boolean isResolved
) {
}
