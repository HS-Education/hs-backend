package com.hs.hstesis.learning.domain.exceptions;

import java.time.LocalDate;

public class DateYearMismatchException extends RuntimeException {
    public DateYearMismatchException(LocalDate date, int expectedYear) {
        super(String.format(
                "Date '%s' does not belong to academic year '%d'.",
                date, expectedYear));
    }
}
