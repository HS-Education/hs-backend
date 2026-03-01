package com.hs.hstesis.learning.domain.exceptions;

public class InvalidGradingPeriodDeleteException extends RuntimeException {
    public InvalidGradingPeriodDeleteException(Integer bimester, String yearName) {
        super(String.format("Cannot delete Bimester %d for Academic Year %s because it has already started or passed.",
                bimester, yearName));
    }
}
