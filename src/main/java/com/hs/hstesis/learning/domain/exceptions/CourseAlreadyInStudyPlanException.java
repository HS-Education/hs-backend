package com.hs.hstesis.learning.domain.exceptions;

public class CourseAlreadyInStudyPlanException extends RuntimeException {
    public CourseAlreadyInStudyPlanException(String courseName, String academicLevelName) {
        super(String.format(
                "Course '%s' is already in the study plan for academic level '%s'.",
                courseName, academicLevelName));
    }
}
