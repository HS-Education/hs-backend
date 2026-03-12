package com.hs.hstesis.learning.domain.exceptions;

public class InvalidGradingPeriodYearException extends RuntimeException {
    public InvalidGradingPeriodYearException(int year) {
        super(String.format("Grading period must be within the academic year '%d'.", year));
    }
}
