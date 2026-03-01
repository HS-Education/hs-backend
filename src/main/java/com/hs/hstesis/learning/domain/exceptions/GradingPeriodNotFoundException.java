package com.hs.hstesis.learning.domain.exceptions;

public class GradingPeriodNotFoundException extends RuntimeException {
    public GradingPeriodNotFoundException(Long id) {
        super(String.format("Grading period with id %d not found", id));
    }
    public GradingPeriodNotFoundException(Integer bimester, Integer year) {
        super(String.format("Bimester %d for Academic Year %d was not found. Please create it first.", bimester, year));
    }
}
