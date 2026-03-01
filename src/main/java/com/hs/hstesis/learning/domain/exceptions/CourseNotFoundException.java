package com.hs.hstesis.learning.domain.exceptions;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long id) {
        super(String.format("Course with id %d not found.", id));
    }
}
