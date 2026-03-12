package com.hs.hstesis.learning.domain.exceptions;

public class FinishedGradingPeriodModificationException extends RuntimeException {
    public FinishedGradingPeriodModificationException(int bimester) {
        super(String.format("Bimester %d has already finished and cannot be modified.", bimester));
    }
}