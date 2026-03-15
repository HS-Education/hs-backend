package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByEducationLevelAndGradeLevelQuery;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByIdQuery;
import com.hs.hstesis.learning.domain.services.StudyPlanQueryService;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.StudyPlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudyPlanQueryServiceImpl implements StudyPlanQueryService {
    private final StudyPlanRepository studyPlanRepository;

    public StudyPlanQueryServiceImpl(StudyPlanRepository studyPlanRepository) {
        this.studyPlanRepository = studyPlanRepository;
    }

    @Override
    public List<StudyPlan> handle(GetStudyPlanByEducationLevelAndGradeLevelQuery query) {
        return studyPlanRepository.findAllByEducationLevelAndGradeLevel(query.educationLevel(), query.gradeLevel());
    }

    @Override
    public Optional<StudyPlan> handle(GetStudyPlanByIdQuery query) {
        return studyPlanRepository.findById(query.studyPlanId());
    }
}
