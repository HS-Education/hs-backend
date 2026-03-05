package com.hs.hstesis.learning.interfaces.rest.resources;

public record GradingPeriodResource(Long id, Long academicYearId, String academicYearName,Integer bimester, String startDate, String endDate, Boolean isActive) {
}
