package com.hs.hstesis.learning.domain.exceptions;

public class CannotDeleteActiveClassroomException extends RuntimeException {
    public CannotDeleteActiveClassroomException(String courseName, String sectionName) {
        super(String.format("Cannot delete active classroom for course '%s' in section '%s'.", courseName, sectionName));
    }
}
