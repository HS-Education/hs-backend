package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class StudyPlanEntryNotFoundException extends ResourceNotFoundException {
    public StudyPlanEntryNotFoundException(Long id) {
        super("Study plan entry", id);
    }

    public StudyPlanEntryNotFoundException() {
        super("No study plans found to generate classrooms.");
    }
}
