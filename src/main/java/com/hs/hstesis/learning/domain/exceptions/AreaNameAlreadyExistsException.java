package com.hs.hstesis.learning.domain.exceptions;

public class AreaNameAlreadyExistsException extends RuntimeException {
    public AreaNameAlreadyExistsException(String name) {
        super(String.format("Area name '%s' already exists.", name));
    }
}
