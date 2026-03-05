package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByAcademicLevelIdAndCourseIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByAcademicLevelIdQuery;

import java.util.List;
import java.util.Optional;

public interface StudyPlanQueryService {
    List<StudyPlan> handle(GetStudyPlanByAcademicLevelIdQuery query);
    Optional<StudyPlan> handle(GetStudyPlanByAcademicLevelIdAndCourseIdQuery query);
}
