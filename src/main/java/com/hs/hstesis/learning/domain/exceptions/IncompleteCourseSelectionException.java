package com.hs.hstesis.learning.domain.exceptions;

public class IncompleteCourseSelectionException extends RuntimeException {
    public IncompleteCourseSelectionException(int missingCount) {
        super(String.format("The classroom generation failed because %d of the selected courses are no longer available in the system. " +
                        "Please refresh your course list and try again.",
                missingCount));
    }
}
