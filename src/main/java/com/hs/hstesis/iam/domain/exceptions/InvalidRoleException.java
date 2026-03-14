package com.hs.hstesis.iam.domain.exceptions;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException(String roleName, String userName) {
        super(String.format("Seed error: Role '%s' is not found for user '%s'.", roleName, userName));
    }
}