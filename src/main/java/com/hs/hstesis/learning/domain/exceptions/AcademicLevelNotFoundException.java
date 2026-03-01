package com.hs.hstesis.learning.domain.exceptions;

public class AcademicLevelNotFoundException extends RuntimeException {
    public AcademicLevelNotFoundException(Long id) {
        super(String.format("Academic level with id %d not found.", id));
    }
}
