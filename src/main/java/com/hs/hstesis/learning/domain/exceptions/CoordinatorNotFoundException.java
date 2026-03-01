package com.hs.hstesis.learning.domain.exceptions;

public class CoordinatorNotFoundException extends RuntimeException {
    public CoordinatorNotFoundException(Long userId) {
        super(String.format("Coordinator with user id %d not found.", userId));
    }
}
