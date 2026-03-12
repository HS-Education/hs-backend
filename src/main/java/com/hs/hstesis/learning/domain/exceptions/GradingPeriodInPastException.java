package com.hs.hstesis.learning.domain.exceptions;

public class GradingPeriodInPastException extends RuntimeException {
    public GradingPeriodInPastException() {
        super("The start date cannot be in the past for a new period.");
    }
}
