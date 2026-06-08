package com.hs.hstesis.achievements.domain.model.valueobjects;

import java.util.List;

public record AreaPerformance(
        Long areaId,
        String areaName,
        Double averageScore,
        List<ClassroomPerformance> classrooms
) {}
