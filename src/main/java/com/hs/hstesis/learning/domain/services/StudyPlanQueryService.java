package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByEducationLevelAndGradeLevelQuery;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByIdQuery;

import java.util.List;
import java.util.Optional;

public interface StudyPlanQueryService {
    List<StudyPlan> handle(GetStudyPlanByEducationLevelAndGradeLevelQuery query);
    Optional<StudyPlan> handle(GetStudyPlanByIdQuery query);
}
