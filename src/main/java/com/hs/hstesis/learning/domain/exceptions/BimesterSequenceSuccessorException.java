package com.hs.hstesis.learning.domain.exceptions;

public class BimesterSequenceSuccessorException extends RuntimeException {
    public BimesterSequenceSuccessorException(int current, int next, java.time.LocalDate startDate) {
        super(String.format("Bimester %d must end before Bimester %d starts (%s).",
                current, next, startDate));
    }
}