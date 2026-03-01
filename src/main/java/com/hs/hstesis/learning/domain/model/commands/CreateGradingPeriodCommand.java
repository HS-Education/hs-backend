package com.hs.hstesis.learning.domain.model.commands;

import java.time.LocalDate;

public record CreateGradingPeriodCommand(Integer bimester, LocalDate startDate, LocalDate endDate, Long academicYearId) {
    public CreateGradingPeriodCommand {
        if (bimester == null || bimester < 1 || bimester > 4) {
            throw new IllegalArgumentException("Bimester must be between 1 and 4");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (academicYearId == null) {
            throw new IllegalArgumentException("Academic Year ID cannot be null");
        }
    }
}
