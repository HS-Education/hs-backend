package com.hs.hstesis.learning.domain.model.commands;

import java.time.LocalDate;

public record UpdateGradingPeriodCommand(Long academicYearId, Long gradingPeriodId, LocalDate startDate, LocalDate endDate) {
    public UpdateGradingPeriodCommand {
        if (academicYearId == null || academicYearId <= 0) {
            throw new IllegalArgumentException("Academic year id cannot be null or negative.");
        }
        if (gradingPeriodId == null || gradingPeriodId <= 0) {
            throw new IllegalArgumentException("Grading period id cannot be null or negative.");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Dates cannot be null.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
    }
}
