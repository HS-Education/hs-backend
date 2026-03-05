package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByAcademicLevelIdAndCourseIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByAcademicLevelIdQuery;
import com.hs.hstesis.learning.domain.services.StudyPlanQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.StudyPlanRepository;
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
    public List<StudyPlan> handle(GetStudyPlanByAcademicLevelIdQuery query) {
        return studyPlanRepository.findAllByAcademicLevelId(query.academicLevelId());
    }

    @Override
    public Optional<StudyPlan> handle(GetStudyPlanByAcademicLevelIdAndCourseIdQuery query) {
        return studyPlanRepository.findByAcademicLevelIdAndCourseId(query.academicLevelId(), query.courseId());
    }
}
