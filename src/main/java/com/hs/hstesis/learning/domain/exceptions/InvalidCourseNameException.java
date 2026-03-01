package com.hs.hstesis.learning.domain.exceptions;

public class InvalidCourseNameException extends RuntimeException {
    public InvalidCourseNameException(String name) {
        super(String.format("The course name '%s' is invalid. It must be between 5 and 20 characters long.", name));
    }
}
