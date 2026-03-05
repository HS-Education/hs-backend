package com.hs.hstesis.learning.domain.exceptions;

public class SectionNotFoundException extends RuntimeException {
    public SectionNotFoundException(Long id) {
        super(String.format("Section with id %d not found.", id));
    }
}
