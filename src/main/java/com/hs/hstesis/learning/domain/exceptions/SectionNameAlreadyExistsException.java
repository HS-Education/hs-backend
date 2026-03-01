package com.hs.hstesis.learning.domain.exceptions;

public class SectionNameAlreadyExistsException extends RuntimeException {
    public SectionNameAlreadyExistsException(String name) {
        super(String.format("Section with name '%s' already exists.", name));
    }
}
