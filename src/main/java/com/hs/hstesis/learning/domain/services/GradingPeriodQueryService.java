package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodsByAcademicYearIdQuery;

import java.util.List;
import java.util.Optional;

public interface GradingPeriodQueryService {
    Optional<GradingPeriod> handle(GetGradingPeriodByIdQuery query);
    List<GradingPeriod> handle(GetGradingPeriodsByAcademicYearIdQuery query);
}
