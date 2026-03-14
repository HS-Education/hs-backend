package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.application.querymodels.AreaWithCoordinator;
import com.hs.hstesis.learning.domain.model.queries.GetAllAreasQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaByIdQuery;

import java.util.List;
import java.util.Optional;

public interface AreaQueryService {
    Optional<AreaWithCoordinator> handle(GetAreaByIdQuery query);
    List<AreaWithCoordinator> handle(GetAllAreasQuery query);
}
