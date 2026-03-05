package com.hs.hstesis.learning.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record CreateGradingPeriodResource(
        Integer bimester,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate,
        Long academicYearId
) {
}