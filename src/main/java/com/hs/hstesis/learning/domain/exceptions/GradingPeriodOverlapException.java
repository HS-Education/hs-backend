package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;

import java.time.LocalDate;

public class GradingPeriodOverlapException extends RuntimeException {
    public GradingPeriodOverlapException(Bimester bimester, LocalDate startDate, LocalDate endDate) {
        super(String.format(
                "Grading period overlaps with bimester '%s' (%s to %s).",
                bimester, startDate, endDate));
    }
}
