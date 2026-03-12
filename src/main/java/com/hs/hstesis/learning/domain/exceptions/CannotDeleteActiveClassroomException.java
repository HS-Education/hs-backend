package com.hs.hstesis.learning.domain.exceptions;

public class CannotDeleteActiveClassroomException extends RuntimeException {
    public CannotDeleteActiveClassroomException() {
        super("The classroom cannot be deleted because the academic cycle has already started.");
    }
}
