package com.hs.hstesis.learning.domain.exceptions;

public class NoAcademicYearReadyException extends RuntimeException {
    public NoAcademicYearReadyException() {
        super("No academic year is ready for classroom generation.");
    }
}
