package com.hs.hstesis.learning.domain.exceptions;

public class CannotDeleteHistoricalDataException extends RuntimeException {
    public CannotDeleteHistoricalDataException(String courseName, Integer year) {
        super(String.format(
                "Cannot delete classroom for course '%s' because it belongs to closed academic year '%d'.",
                courseName, year));
    }
}
