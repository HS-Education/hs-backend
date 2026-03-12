package com.hs.hstesis.learning.domain.exceptions;

public class AcademicYearIsActiveException extends RuntimeException {
    public AcademicYearIsActiveException() {
        super("Operation cannot be performed because the academic year is currently active.");
    }
}
