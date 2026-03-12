package com.hs.hstesis.learning.domain.exceptions;

public class StartedGradingPeriodModificationException extends RuntimeException {
    public StartedGradingPeriodModificationException(int bimester) {
        super(String.format("The start date for Bimester '%d' cannot be modified because the period has already begun.",
                bimester));
    }
}