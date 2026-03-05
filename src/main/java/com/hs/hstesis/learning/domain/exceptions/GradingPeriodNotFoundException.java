package com.hs.hstesis.learning.domain.exceptions;

public class GradingPeriodNotFoundException extends RuntimeException {
    public GradingPeriodNotFoundException(Long id) {
        super(String.format("Grading period with id %d not found.", id));
    }

    public GradingPeriodNotFoundException(Integer bimester, Integer year) {
        super(String.format(
                "Grading period for bimester '%d' in academic year '%d' not found.",
                bimester, year));
    }
}
