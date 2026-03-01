package com.hs.hstesis.learning.domain.exceptions;

public class CannotDeleteActiveClassroomException extends RuntimeException {
    public CannotDeleteActiveClassroomException(String courseName, String sectionName) {
        super(String.format(
                "The classroom for '%s' in section '%s' is currently active. " +
                        "Active classrooms cannot be deleted to prevent loss of academic records.",
                courseName, sectionName));
    }
}
