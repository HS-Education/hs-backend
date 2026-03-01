package com.hs.hstesis.learning.domain.exceptions;

public class StudyPlanNotConfiguredException extends RuntimeException {
    public StudyPlanNotConfiguredException(String academicLevelName) {
        super(String.format("The study plan for the academic level '%s' is not configured. " + "Please configure the study plan before generating classrooms.",
                academicLevelName));
    }
}
