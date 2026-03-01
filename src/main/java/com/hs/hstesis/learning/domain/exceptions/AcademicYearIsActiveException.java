package com.hs.hstesis.learning.domain.exceptions;

public class AcademicYearIsActiveException extends RuntimeException {
    public AcademicYearIsActiveException(Long id) {
        super(String.format("Academic year with id %d is active and cannot be deleted.", id));
    }
}
