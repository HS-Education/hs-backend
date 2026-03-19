package com.hs.hstesis.repo.domain.exceptions;

public class CoordinatorDoesNotOwnCourseException extends RuntimeException {
    public CoordinatorDoesNotOwnCourseException() {
        super("Coordinator does not belong to this course area.");
    }
}
