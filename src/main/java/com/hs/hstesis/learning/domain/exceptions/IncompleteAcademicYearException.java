package com.hs.hstesis.learning.domain.exceptions;

public class IncompleteAcademicYearException extends RuntimeException {
    public IncompleteAcademicYearException(Integer year, long count) {
        super(String.format("Academic Year %d cannot be activated. It requires 4 grading periods, but only %d were found.", year, count));
    }
}
