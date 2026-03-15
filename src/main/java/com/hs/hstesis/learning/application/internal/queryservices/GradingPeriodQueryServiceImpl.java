package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodsByAcademicYearIdQuery;
import com.hs.hstesis.learning.domain.services.GradingPeriodQueryService;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.GradingPeriodRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GradingPeriodQueryServiceImpl implements GradingPeriodQueryService {
    private final GradingPeriodRepository gradingPeriodRepository;

    public GradingPeriodQueryServiceImpl(GradingPeriodRepository gradingPeriodRepository) {
        this.gradingPeriodRepository = gradingPeriodRepository;
    }

    @Override
    public Optional<GradingPeriod> handle(GetGradingPeriodByIdQuery query){
        return gradingPeriodRepository.findById(query.id());
    }

    @Override
    public List<GradingPeriod> handle(GetGradingPeriodsByAcademicYearIdQuery query) {
        return gradingPeriodRepository.findAllByAcademicYearId(query.academicYearId());
    }
}
