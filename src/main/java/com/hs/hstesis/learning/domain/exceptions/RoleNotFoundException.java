package com.hs.hstesis.learning.domain.exceptions;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String roleName) {
        super(String.format("Role %s not found", roleName));
    }
}
