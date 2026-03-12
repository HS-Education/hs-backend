package com.hs.hstesis.learning.domain.exceptions;

public class AcademicYearNotFoundException extends RuntimeException {
    public AcademicYearNotFoundException(Integer year) {
        super(String.format("Academic year '%d' not found.", year));
    }
}
