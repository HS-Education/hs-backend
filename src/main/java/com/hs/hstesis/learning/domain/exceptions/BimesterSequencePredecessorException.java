package com.hs.hstesis.learning.domain.exceptions;

import java.time.LocalDate;

public class BimesterSequencePredecessorException extends RuntimeException {
    public BimesterSequencePredecessorException(int current, int previous, LocalDate endDate) {
        super(String.format("Bimester %d must start after Bimester %d ends (%s).",
                current, previous, endDate));
    }
}
