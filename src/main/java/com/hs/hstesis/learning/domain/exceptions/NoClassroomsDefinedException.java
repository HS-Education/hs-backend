package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;

public class NoClassroomsDefinedException extends RuntimeException {
    public NoClassroomsDefinedException(EducationLevel educationLevel, GradeLevel gradeLevel, int year) {
        super(String.format("No classrooms have been generated for %s - %s in the academic year %d.",
                educationLevel, gradeLevel, year));
    }
}