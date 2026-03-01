package com.hs.hstesis.learning.domain.exceptions;

public class ClassroomAlreadyExistsException extends RuntimeException {
    public ClassroomAlreadyExistsException(String courseName, String sectionName, Integer year) {
        super(String.format("Classroom for course '%s', section '%s', year '%d' already exists.", courseName, sectionName, year));
    }
}
