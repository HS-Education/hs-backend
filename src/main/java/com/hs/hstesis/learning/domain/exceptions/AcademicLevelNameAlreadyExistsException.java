package com.hs.hstesis.learning.domain.exceptions;

public class AcademicLevelNameAlreadyExistsException extends RuntimeException {
    public AcademicLevelNameAlreadyExistsException(String name) {
        super(String.format("An academic level with the name '%s' already exists", name));
    }
}
