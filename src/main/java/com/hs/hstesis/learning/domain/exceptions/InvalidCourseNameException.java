package com.hs.hstesis.learning.domain.exceptions;

public class InvalidCourseNameException extends RuntimeException {
    public InvalidCourseNameException(String name) {
        super(String.format("Invalid course name '%s'. It must contain between 5 and 50 characters.", name));
    }
}
