package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.commands.CreateAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateAreaCommand;
import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.domain.services.AcademicYearStateValidator;
import com.hs.hstesis.learning.domain.services.AreaCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AreaRepository;
import com.hs.hstesis.learning.infrastructure.jpa.CourseRepository;
import com.hs.hstesis.shared.domain.model.util.TextUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AreaCommandServiceImpl implements AreaCommandService {
    private final AreaRepository areaRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearStateValidator yearValidator;
    private final IamContextFacade iamContextFacade;

    public AreaCommandServiceImpl(AreaRepository areaRepository,
                                  CourseRepository courseRepository,
                                  AcademicYearStateValidator yearValidator,
                                  IamContextFacade iamContextFacade) {
        this.areaRepository = areaRepository;
        this.courseRepository = courseRepository;
        this.yearValidator = yearValidator;
        this.iamContextFacade = iamContextFacade;
    }

    @Override
    public Long handle(CreateAreaCommand command){

        String nameToCreate = TextUtils.normalize(command.name());

        boolean alreadyExists = areaRepository.findAll().stream()
                .anyMatch(a -> TextUtils.normalize(a.getName()).equals(nameToCreate));

        if (alreadyExists) {
            throw new AreaNameAlreadyExistsException(nameToCreate);
        }

        if (command.coordinatorId() != null) {
            validateCoordinator(command.coordinatorId(), null);
        }

        var area = new Area(command);
        areaRepository.save(area);
        return area.getId();
    }

    @Override
    public Optional<Area> handle(UpdateAreaCommand command) {
        var area = areaRepository.findById(command.id())
                .orElseThrow(() -> new AreaNotFoundException(command.id()));

        if (command.name() != null) {
            String newNormalizedName = TextUtils.normalize(command.name());
            String currentNormalizedName = TextUtils.normalize(area.getName());

            if (!newNormalizedName.equals(currentNormalizedName)) {
                boolean alreadyExists = areaRepository.findAll().stream()
                        .anyMatch(a -> TextUtils.normalize(a.getName()).equals(newNormalizedName));

                if (alreadyExists) {
                    throw new AreaNameAlreadyExistsException(newNormalizedName);
                }
            }
        }

        if (command.coordinatorId() != null) {
            validateCoordinator(command.coordinatorId(), area.getId());
        }

        area.update(command);
        areaRepository.save(area);
        return Optional.of(area);
    }

    @Override
    public void handle(DeleteAreaCommand command){
        yearValidator.validateCurrentYearIsNotActive();

        var area = areaRepository.findById(command.id())
                .orElseThrow(() -> new AreaNotFoundException(command.id()));

        if (courseRepository.existsByAreaId(area.getId())) {
            throw new AreaRelatedToCoursesException(area.getName());
        }

        areaRepository.deleteById(command.id());
    }

    private void validateCoordinator(Long userId, Long currentAreaId) {
        String userName = iamContextFacade.fetchUserNameById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!iamContextFacade.hasRole(userId, "COORDINATOR")) {
            throw new InvalidUserRoleException(userName, "COORDINATOR");
        }

        areaRepository.findByCoordinatorId(userId).ifPresent(existingArea -> {
            if (!existingArea.getId().equals(currentAreaId)) {
                throw new UserAlreadyIsACoordinatorException(userName, existingArea.getName());
            }
        });
    }
}
