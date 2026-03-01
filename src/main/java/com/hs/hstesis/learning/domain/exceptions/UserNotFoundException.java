package com.hs.hstesis.learning.domain.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super(String.format("Teacher with ID %d not found", userId));
    }
}
