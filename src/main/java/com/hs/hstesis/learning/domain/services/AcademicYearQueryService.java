package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.model.queries.GetAcademicYearByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAllAcademicYearsQuery;

import java.util.List;
import java.util.Optional;

public interface AcademicYearQueryService {
    Optional<AcademicYear> handle(GetAcademicYearByIdQuery query);
    List<AcademicYear> handle(GetAllAcademicYearsQuery query);
}