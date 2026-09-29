package com.hs.hstesis.learning.domain.exceptions;

public class GradingPeriodDurationTooShortException extends RuntimeException {
    public GradingPeriodDurationTooShortException(Long weeks) {
        super(String.format("A grading period must last at least 3 weeks. Current duration: '%d' weeks.", weeks));
    }
}
