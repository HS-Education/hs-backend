package com.hs.hstesis.learning.domain.exceptions;

public class UserAlreadyIsACoordinatorException extends RuntimeException {
    public UserAlreadyIsACoordinatorException(String userName, String areaName) {
        super(String.format("User '%s' is already assigned as a coordinator for the area '%s'.",
                userName, areaName));
    }
}
