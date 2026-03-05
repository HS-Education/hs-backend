package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.iam.domain.model.valueobjects.Roles;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.RoleRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.UserRepository;
import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.commands.AssignAreaCoordinatorCommand;
import com.hs.hstesis.learning.domain.model.commands.ReassignAreaCoordinatorCommand;
import com.hs.hstesis.learning.domain.model.entities.AreaCoordinator;
import com.hs.hstesis.learning.domain.services.AreaCoordinatorCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AreaCoordinatorRepository;
import com.hs.hstesis.learning.infrastructure.jpa.AreaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AreaCoordinatorCommandServiceImpl implements AreaCoordinatorCommandService {
    private final AreaCoordinatorRepository areaCoordinatorRepository;
    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final RoleRepository roleRepository;

    public AreaCoordinatorCommandServiceImpl(AreaCoordinatorRepository areaCoordinatorRepository,
                                             UserRepository userRepository,
                                             AreaRepository areaRepository,
                                             RoleRepository roleRepository) {
        this.areaCoordinatorRepository = areaCoordinatorRepository;
        this.userRepository = userRepository;
        this.areaRepository = areaRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Long handle(AssignAreaCoordinatorCommand command){
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));
        var area = areaRepository.findById(command.areaId())
                .orElseThrow(() -> new AreaNotFoundException(command.areaId()));

        if (areaCoordinatorRepository.existsByAreaId(area.getId())) {
            throw new AreaAlreadyHasCoordinatorException(area.getName());
        }

        var coordinator = new AreaCoordinator(user, area);
        areaCoordinatorRepository.save(coordinator);
        return coordinator.getId();
    }

    @Override
    @Transactional
    public Long handle(ReassignAreaCoordinatorCommand command) {
        var coordinatorRole = roleRepository.findByRoleName(Roles.ROLE_COORDINATOR)
                .orElseThrow(() -> new RoleNotFoundException(Roles.ROLE_COORDINATOR));

        var newTeacher = userRepository.findById(command.newCoordinatorId())
                .orElseThrow(() -> new UserNotFoundException(command.newCoordinatorId()));
        var area = areaRepository.findById(command.areaId())
                .orElseThrow(() -> new AreaNotFoundException(command.areaId()));

        areaCoordinatorRepository.findByAreaId(area.getId()).ifPresent(oldAssignment -> {
            var oldTeacher = oldAssignment.getUser();

            boolean coordinatesOtherAreas = areaCoordinatorRepository
                    .existsByUserIdAndAreaIdNot(oldTeacher.getId(), area.getId());

            if (!coordinatesOtherAreas) {
                oldTeacher.removeRole(coordinatorRole);
                userRepository.save(oldTeacher);
            }

            areaCoordinatorRepository.delete(oldAssignment);
        });

        newTeacher.addRole(coordinatorRole);
        userRepository.save(newTeacher);

        var newAssignment = new AreaCoordinator(newTeacher, area);
        areaCoordinatorRepository.save(newAssignment);

        return newAssignment.getId();
    }
}
