package com.hs.hstesis.learning.domain.exceptions;

public class NoActiveAcademicYearException extends RuntimeException {
    public NoActiveAcademicYearException() {
        super("No active academic year found.");
    }
}
