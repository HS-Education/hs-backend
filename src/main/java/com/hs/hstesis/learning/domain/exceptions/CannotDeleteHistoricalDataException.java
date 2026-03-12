package com.hs.hstesis.learning.domain.exceptions;

public class CannotDeleteHistoricalDataException extends RuntimeException {
    public CannotDeleteHistoricalDataException() {
        super("Cannot delete historical data");
    }
}
