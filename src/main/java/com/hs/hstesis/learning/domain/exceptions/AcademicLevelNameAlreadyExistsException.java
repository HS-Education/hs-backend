package com.hs.hstesis.learning.domain.exceptions;

public class AcademicLevelNameAlreadyExistsException extends RuntimeException {
    public AcademicLevelNameAlreadyExistsException(String name) {
        super(String.format("Academic level '%s' already exists.", name));
    }
}
