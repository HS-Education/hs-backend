package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.domain.model.queries.GetAllAreasQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaByIdQuery;

import java.util.List;
import java.util.Optional;

public interface AreaQueryService {
    Optional<Area> handle(GetAreaByIdQuery query);
    List<Area> handle(GetAllAreasQuery query);
}
