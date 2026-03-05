package com.hs.hstesis.learning.domain.exceptions;

import java.time.LocalDate;

public class GradingPeriodOverlapException extends RuntimeException {
    public GradingPeriodOverlapException(Integer bimester, LocalDate startDate, LocalDate endDate) {
        super(String.format(
                "Grading period overlaps with bimester '%d' (%s to %s).",
                bimester, startDate, endDate));
    }
}
