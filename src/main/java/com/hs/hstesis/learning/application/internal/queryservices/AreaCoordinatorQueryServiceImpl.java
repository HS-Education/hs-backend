package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.entities.AreaCoordinator;
import com.hs.hstesis.learning.domain.model.queries.GetAreaCoordinatorByIdQuery;
import com.hs.hstesis.learning.domain.services.AreaCoordinatorQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.AreaCoordinatorRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AreaCoordinatorQueryServiceImpl implements AreaCoordinatorQueryService {
    private final AreaCoordinatorRepository areaCoordinatorRepository;

    public AreaCoordinatorQueryServiceImpl(AreaCoordinatorRepository areaCoordinatorRepository) {
        this.areaCoordinatorRepository = areaCoordinatorRepository;
    }

    @Override
    public Optional<AreaCoordinator> handle(GetAreaCoordinatorByIdQuery query){
        return areaCoordinatorRepository.findById(query.id());
    }
}
