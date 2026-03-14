package com.hs.hstesis.iam.domain.exceptions;

public class CannotRemoveLastAdminException extends RuntimeException {
    public CannotRemoveLastAdminException() {
        super("Cannot remove the last admin user. At least one admin must remain in the system.");
    }
}
