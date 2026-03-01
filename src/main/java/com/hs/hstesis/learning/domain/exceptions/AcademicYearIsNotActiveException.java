package com.hs.hstesis.learning.domain.exceptions;

public class AcademicYearIsNotActiveException extends RuntimeException {
    public AcademicYearIsNotActiveException(Integer year) {
        super(String.format("Academic year %d is not active.", year));
    }
}
