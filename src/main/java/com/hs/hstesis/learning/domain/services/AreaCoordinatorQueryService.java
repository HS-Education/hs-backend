package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.entities.AreaCoordinator;
import com.hs.hstesis.learning.domain.model.queries.GetAreaCoordinatorByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaCoordinatorByUserIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaCoordinatorByAreaIdQuery;

import java.util.Optional;

public interface AreaCoordinatorQueryService {
    Optional<AreaCoordinator> handle(GetAreaCoordinatorByIdQuery query);
    Optional<AreaCoordinator> handle(GetAreaCoordinatorByAreaIdQuery query);
    Optional<AreaCoordinator> handle(GetAreaCoordinatorByUserIdQuery query);
}
