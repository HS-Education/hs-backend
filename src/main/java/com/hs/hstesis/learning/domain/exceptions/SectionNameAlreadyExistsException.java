package com.hs.hstesis.learning.domain.exceptions;

public class SectionNameAlreadyExistsException extends RuntimeException {
    public SectionNameAlreadyExistsException(String name) {
        super(String.format("Section name '%s' already exists.", name));
    }
}
