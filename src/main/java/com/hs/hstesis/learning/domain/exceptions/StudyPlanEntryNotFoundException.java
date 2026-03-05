package com.hs.hstesis.learning.domain.exceptions;

public class StudyPlanEntryNotFoundException extends RuntimeException {
    public StudyPlanEntryNotFoundException(Long academicLevelId, Long courseId) {
        super(String.format(
                "Study plan entry with academic level id %d and course id %d not found.",
                academicLevelId, courseId));
    }
}
