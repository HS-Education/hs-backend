package com.hs.hstesis.learning.domain.exceptions;

public class InvalidUserRoleException extends RuntimeException {
    public InvalidUserRoleException(String userName, String requiredRole) {
        super(String.format("User '%s' does not have the required role: %s", userName, requiredRole));
    }
}
