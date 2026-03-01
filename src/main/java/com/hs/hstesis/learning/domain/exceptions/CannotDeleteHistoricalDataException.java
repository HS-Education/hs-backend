package com.hs.hstesis.learning.domain.exceptions;

public class CannotDeleteHistoricalDataException extends RuntimeException {
    public CannotDeleteHistoricalDataException(String message) {
        super(message);
    }

    public CannotDeleteHistoricalDataException(String courseName, Integer year) {
        super(String.format("Cannot delete the classroom for '%s' because it belongs to the closed academic year %d.",
                courseName, year));
    }
}
