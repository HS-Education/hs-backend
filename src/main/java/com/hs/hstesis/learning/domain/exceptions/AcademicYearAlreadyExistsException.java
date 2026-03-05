package com.hs.hstesis.learning.domain.exceptions;

public class AcademicYearAlreadyExistsException extends RuntimeException {
    public AcademicYearAlreadyExistsException(Integer year) {
        super(String.format("Academic year '%d' already exists.", year));
    }
}
