package com.hs.hstesis.learning.domain.exceptions;

public class InvalidAreaNameException extends RuntimeException {
    public InvalidAreaNameException(String name) {
        super(String.format("The area name '%s' is invalid. It must be between 5 and 20 characters long.", name));
    }
}
