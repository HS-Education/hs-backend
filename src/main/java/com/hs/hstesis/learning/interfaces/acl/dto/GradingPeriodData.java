package com.hs.hstesis.learning.interfaces.acl.dto;

import java.time.LocalDate;

public record GradingPeriodData(
        Long id,
        String bimester,
        LocalDate startDate,
        LocalDate endDate
) {
}
