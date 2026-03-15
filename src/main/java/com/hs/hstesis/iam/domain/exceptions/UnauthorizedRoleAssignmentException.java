package com.hs.hstesis.iam.domain.exceptions;

public class UnauthorizedRoleAssignmentException extends RuntimeException {
    public UnauthorizedRoleAssignmentException(String roleName) {
        super(String.format("%s cannot be assigned manually", roleName));
    }
}
