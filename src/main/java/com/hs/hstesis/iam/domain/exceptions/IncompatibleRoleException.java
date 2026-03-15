package com.hs.hstesis.iam.domain.exceptions;

public class IncompatibleRoleException extends RuntimeException {
    public IncompatibleRoleException(String newRole, String existingRole) {
        super(String.format("Cannot add role '%s' to user with existing role '%s'", newRole, existingRole));
    }
}
