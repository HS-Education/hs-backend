package com.hs.hstesis.learning.domain.exceptions;

public class InvalidGradingPeriodDeleteException extends RuntimeException {
    public InvalidGradingPeriodDeleteException(Integer bimester, String yearName) {
        super(String.format("Cannot delete bimester %d for academic year %s because it has already started or ended.",
                bimester, yearName));
    }
}
