package com.hs.hstesis.repo.domain.exceptions;

public class CoordinatorNotAssignedToAnyAreaException extends RuntimeException {
    public CoordinatorNotAssignedToAnyAreaException() {
        super("Coordinator is not assigned to any area");
    }
}

