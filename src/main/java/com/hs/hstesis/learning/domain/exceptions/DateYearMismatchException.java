package com.hs.hstesis.learning.domain.exceptions;

import java.time.LocalDate;

public class DateYearMismatchException extends RuntimeException {
    public DateYearMismatchException(LocalDate date, int expectedYear) {
        super(String.format("The date %s does not belong to the academic year %d.", date, expectedYear));
    }
}
