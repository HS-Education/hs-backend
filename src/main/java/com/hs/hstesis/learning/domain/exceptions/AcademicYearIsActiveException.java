package com.hs.hstesis.learning.domain.exceptions;

public class AcademicYearIsActiveException extends RuntimeException {
    public AcademicYearIsActiveException(Integer year) {
        super(String.format("Academic year '%d' cannot be deleted because it is active.", year));
    }
}
