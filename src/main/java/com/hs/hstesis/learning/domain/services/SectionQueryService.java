package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.domain.model.queries.GetAllSectionsQuery;
import com.hs.hstesis.learning.domain.model.queries.GetSectionByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetSectionsByAcademicLevelIdQuery;

import java.util.List;
import java.util.Optional;

public interface SectionQueryService {
    Optional<Section> handle(GetSectionByIdQuery query);
    List<Section> handle(GetAllSectionsQuery query);
    List<Section> handle(GetSectionsByAcademicLevelIdQuery query);
}
