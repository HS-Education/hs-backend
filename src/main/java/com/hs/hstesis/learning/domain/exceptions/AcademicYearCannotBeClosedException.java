package com.hs.hstesis.learning.domain.exceptions;

import java.time.LocalDate;

public class AcademicYearCannotBeClosedException extends RuntimeException {
    public AcademicYearCannotBeClosedException(Integer year, LocalDate endDate) {
        super(String.format("Academic Year %d cannot be closed yet. The last grading period ends on %s.", year, endDate));
    }
}
