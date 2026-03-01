package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByAcademicLevelIdQuery;

import java.util.List;

public interface StudyPlanQueryService {
    List<StudyPlan> handle(GetStudyPlanByAcademicLevelIdQuery query);
}
