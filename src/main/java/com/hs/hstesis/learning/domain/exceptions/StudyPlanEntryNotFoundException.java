package com.hs.hstesis.learning.domain.exceptions;

public class StudyPlanEntryNotFoundException extends RuntimeException {
    public StudyPlanEntryNotFoundException(Long id) {
        super(String.format("Study plan with id '%d' not found.", id));
    }
    public StudyPlanEntryNotFoundException(){
        super("No study plans found to generate classrooms.");
    }
}
