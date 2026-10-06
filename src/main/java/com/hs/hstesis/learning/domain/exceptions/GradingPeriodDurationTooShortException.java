package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;

public class GradingPeriodDurationTooShortException extends RuntimeException {
    public GradingPeriodDurationTooShortException(Long weeks) {
        super(String.format("A grading period must last at least %d weeks. Current duration: '%d' weeks.",
                GradingPeriod.MINIMUM_DURATION_WEEKS, weeks));
    }
}
