package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.iam.infrastructure.persistance.jpa.UserRepository;
import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.commands.AssignAreaCoordinatorCommand;
import com.hs.hstesis.learning.domain.model.commands.UnassignAreaCoordinatorCommand;
import com.hs.hstesis.learning.domain.model.entities.AreaCoordinator;
import com.hs.hstesis.learning.domain.services.AreaCoordinatorCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AreaCoordinatorRepository;
import com.hs.hstesis.learning.infrastructure.jpa.AreaRepository;
import org.springframework.stereotype.Service;

@Service
public class AreaCoordinatorCommandServiceImpl implements AreaCoordinatorCommandService {
    private final AreaCoordinatorRepository areaCoordinatorRepository;
    private final UserRepository userRepository;
    private final AreaRepository areaRepository;

    public AreaCoordinatorCommandServiceImpl(AreaCoordinatorRepository areaCoordinatorRepository, UserRepository userRepository, AreaRepository areaRepository) {
        this.areaCoordinatorRepository = areaCoordinatorRepository;
        this.userRepository = userRepository;
        this.areaRepository = areaRepository;
    }

    @Override
    public void handle(AssignAreaCoordinatorCommand command){
        if(areaCoordinatorRepository.existsByUserIdAndAreaId(command.userId(), command.areaId())){
            throw new UserAlreadyAssignedToAreaException(command.userId(), command.areaId());
        }

        if (areaCoordinatorRepository.existsByAreaId(command.areaId())) {
            throw new AreaAlreadyHasCoordinatorException(command.areaId());
        }

        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new CoordinatorNotFoundException(command.userId()));
        var area = areaRepository.findById(command.areaId())
                .orElseThrow(() -> new AreaNotFoundException(command.areaId()));

        var coordinator = new AreaCoordinator(user, area);
        areaCoordinatorRepository.save(coordinator);
    }

    @Override
    public void handle(UnassignAreaCoordinatorCommand command){
        var coordinator = areaCoordinatorRepository.findByUserIdAndAreaId(command.userId(), command.areaId())
                .orElseThrow(() -> new AreaCoordinatorNotFoundException(command.userId(), command.areaId()));

        areaCoordinatorRepository.delete(coordinator);
    }
}
