package com.hs.hstesis.learning.domain.exceptions;

public class AcademicYearNotFoundException extends RuntimeException {
    public AcademicYearNotFoundException(Long id) {
        super(String.format("Academic year with id %d not found.", id));
    }
}
