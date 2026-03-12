package com.hs.hstesis.learning.domain.exceptions;

public class CourseNameAlreadyException extends RuntimeException {
    public CourseNameAlreadyException(String name) {
        super(String.format("Course name '%s' already exists.", name));
    }
}
