package com.hs.hstesis.achievements.domain.model.valueobjects;

import java.util.List;

public record ClassroomPerformance(
        Long classroomId,
        String classroomName,
        Double averageScore,
        List<StudentPerformance> students
) {}
