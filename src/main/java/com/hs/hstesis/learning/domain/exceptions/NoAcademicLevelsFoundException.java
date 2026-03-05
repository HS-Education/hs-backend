package com.hs.hstesis.learning.domain.exceptions;

public class NoAcademicLevelsFoundException extends RuntimeException {
    public NoAcademicLevelsFoundException() {
        super("No academic levels were found.");
    }
}
