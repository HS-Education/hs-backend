package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class GradingPeriodNotFoundException extends ResourceNotFoundException {
    public GradingPeriodNotFoundException(Long id) {
        super("Grading period", id);
    }
}
