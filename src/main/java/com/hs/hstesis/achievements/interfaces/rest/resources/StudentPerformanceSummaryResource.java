package com.hs.hstesis.achievements.interfaces.rest.resources;

import java.util.List;

public record StudentPerformanceSummaryResource(
        Long studentId,
        Long gradingPeriodId,
        Double bimesterAverage,
        List<WeeklyPerformanceResource> weeklyProgression,
        List<DefinitiveGradeResource> definitiveGrades
) {
    public record WeeklyPerformanceResource(
            Integer weekNumber,
            Double averageScore,
            Boolean needsRemedial
    ) {}

    public record DefinitiveGradeResource(
            Long questionnaireId,
            Integer weekNumber,
            Integer score
    ) {}
}
