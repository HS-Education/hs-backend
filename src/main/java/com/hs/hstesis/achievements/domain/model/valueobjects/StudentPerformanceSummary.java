package com.hs.hstesis.achievements.domain.model.valueobjects;

import java.util.List;

public record StudentPerformanceSummary(
        Long studentId,
        Long gradingPeriodId,
        Double bimesterAverage,
        List<WeeklyPerformance> weeklyProgression,
        List<DefinitiveGrade> definitiveGrades
) {
    public record WeeklyPerformance(
            Integer weekNumber,
            Double averageScore,
            Boolean needsRemedial
    ) {}

    public record DefinitiveGrade(
            Long questionnaireId,
            Integer weekNumber,
            Integer score
    ) {}
}
