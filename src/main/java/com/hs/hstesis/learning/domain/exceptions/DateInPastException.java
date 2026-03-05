package com.hs.hstesis.learning.domain.exceptions;

import java.time.LocalDate;

public class DateInPastException extends RuntimeException {
    public DateInPastException(LocalDate date) {
        super(String.format("Date '%s' cannot be in the past.", date));
    }
}
