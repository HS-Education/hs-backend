package com.hs.hstesis.iam.domain.exceptions;

public class RoleInUseException extends RuntimeException {
    public RoleInUseException(String roleName, String dependencyContext) {
        super(String.format("Cannot remove role '%s' because the user is currently assigned to an active %s.", roleName, dependencyContext));
    }
}
