package com.hs.hstesis.learning.domain.exceptions;

public class UserAlreadyAssignedToAreaException extends RuntimeException {
    public UserAlreadyAssignedToAreaException(Long userId, Long areaId) {
        super(String.format("User with ID %d is already assigned to area with ID %d", userId, areaId));
    }
}
