package com.hs.hstesis.learning.domain.exceptions;

public class UserIsNotACoordinatorException extends RuntimeException {
    public UserIsNotACoordinatorException(String userName) {
        super(String.format("User '%s' is not a coordinator", userName));
    }
}
