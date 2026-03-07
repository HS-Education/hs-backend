package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.entities.AreaCoordinator;
import com.hs.hstesis.learning.domain.model.queries.GetAreaCoordinatorByIdQuery;

import java.util.Optional;

public interface AreaCoordinatorQueryService {
    Optional<AreaCoordinator> handle(GetAreaCoordinatorByIdQuery query);
}
