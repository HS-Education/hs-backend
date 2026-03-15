package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.learning.application.querymodels.AreaWithCoordinatorQueryModel;
import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.domain.model.queries.ExistsAreaByCoordinatorIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAllAreasQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaByIdQuery;
import com.hs.hstesis.learning.domain.services.AreaQueryService;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.AreaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AreaQueryServiceImpl implements AreaQueryService {
    private final AreaRepository areaRepository;
    private final IamContextFacade iamContextFacade;

    public AreaQueryServiceImpl(AreaRepository areaRepository,
                                IamContextFacade iamContextFacade) {
        this.areaRepository = areaRepository;
        this.iamContextFacade = iamContextFacade;
    }

    @Override
    public Optional<AreaWithCoordinatorQueryModel> handle(GetAreaByIdQuery query) {

        return areaRepository.findById(query.id())
                .map(area -> {
                    var username = iamContextFacade
                            .fetchUserNameById(area.getCoordinatorId())
                            .orElse("Unknown");

                    return new AreaWithCoordinatorQueryModel(area, username);
                });
    }


    @Override
    public List<AreaWithCoordinatorQueryModel> handle(GetAllAreasQuery query) {
        var areas = areaRepository.findAll();

        var coordinatorIds = areas.stream()
                .map(Area::getCoordinatorId)
                .collect(Collectors.toSet());

        var usernames = iamContextFacade.fetchUserNamesByIds(coordinatorIds);

        return areas.stream()
                .map(area -> new AreaWithCoordinatorQueryModel(
                        area,
                        usernames.getOrDefault(area.getCoordinatorId(), "Unknown")
                ))
                .toList();
    }

    @Override
    public boolean handle(ExistsAreaByCoordinatorIdQuery query){
        return areaRepository.existsByCoordinatorId(query.coordinatorId());
    }
}
