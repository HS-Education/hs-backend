package com.hs.hstesis.learning.domain.exceptions;

public class AreaNotFoundException extends RuntimeException {
    public AreaNotFoundException(Long id) {
        super(String.format("Area with id %d not found.", id));
    }
}
