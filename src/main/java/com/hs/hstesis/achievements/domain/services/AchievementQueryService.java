package com.hs.hstesis.achievements.domain.services;

import com.hs.hstesis.achievements.domain.model.valueobjects.AreaPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.ClassroomPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformance;

import com.hs.hstesis.achievements.domain.model.queries.GetAreaPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetClassroomPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceQuery;

import java.util.Optional;

public interface AchievementQueryService {
    Optional<StudentPerformance> handle(GetStudentPerformanceQuery query);
    Optional<com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformanceSummary> handle(com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceSummaryQuery query);
    Optional<ClassroomPerformance> handle(GetClassroomPerformanceQuery query);
    Optional<AreaPerformance> handle(GetAreaPerformanceQuery query);
}
