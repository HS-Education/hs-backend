package com.hs.hstesis.learning.domain.exceptions;

public class StudyPlanEntryNotFoundException extends RuntimeException {
    public StudyPlanEntryNotFoundException(Long studyPlanId) {
        super(String.format("Study Plan entry with ID: %s not found", studyPlanId));
    }
}
