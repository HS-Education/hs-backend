package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;
import com.hs.hstesis.learning.domain.model.queries.GetAcademicLevelByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAllAcademicLevelsQuery;

import java.util.List;
import java.util.Optional;

public interface AcademicLevelQueryService {
    Optional<AcademicLevel> handle(GetAcademicLevelByIdQuery query);
    List<AcademicLevel> handle(GetAllAcademicLevelsQuery query);
}
