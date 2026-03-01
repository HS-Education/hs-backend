package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.domain.model.queries.GetAllAreasQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaByIdQuery;
import com.hs.hstesis.learning.domain.services.AreaQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.AreaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AreaQueryServiceImpl implements AreaQueryService {
    private final AreaRepository areaRepository;

    public AreaQueryServiceImpl(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @Override
    public Optional<Area> handle(GetAreaByIdQuery query) {
        return areaRepository.findById(query.id());
    }

    @Override
    public List<Area> handle(GetAllAreasQuery query) {
        return areaRepository.findAll();
    }
}
