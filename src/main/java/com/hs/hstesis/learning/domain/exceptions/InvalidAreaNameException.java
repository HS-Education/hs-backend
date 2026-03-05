package com.hs.hstesis.learning.domain.exceptions;

public class InvalidAreaNameException extends RuntimeException {
    public InvalidAreaNameException(String name) {
        super(String.format("Invalid area name '%s'. It must contain between 5 and 20 characters.", name));
    }
}
