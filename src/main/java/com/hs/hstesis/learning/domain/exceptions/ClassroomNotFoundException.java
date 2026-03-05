package com.hs.hstesis.learning.domain.exceptions;

public class ClassroomNotFoundException extends RuntimeException {
    public ClassroomNotFoundException(Long id) {
        super(String.format("Classroom with id %d not found.", id));
    }
}
