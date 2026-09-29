package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.application.querymodels.AreaWithCoordinatorQueryModel;
import com.hs.hstesis.learning.domain.model.queries.ExistsAreaByCoordinatorIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAllAreasQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaByCoordinatorIdQuery;

import java.util.List;
import java.util.Optional;

public interface AreaQueryService {
    Optional<AreaWithCoordinatorQueryModel> handle(GetAreaByIdQuery query);
    Optional<AreaWithCoordinatorQueryModel> handle(GetAreaByCoordinatorIdQuery query);
    List<AreaWithCoordinatorQueryModel> handle(GetAllAreasQuery query);
    boolean handle(ExistsAreaByCoordinatorIdQuery query);
}
