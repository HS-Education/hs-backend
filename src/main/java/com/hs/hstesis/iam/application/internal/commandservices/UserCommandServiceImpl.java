package com.hs.hstesis.iam.application.internal.commandservices;

import com.hs.hstesis.iam.domain.exceptions.*;
import com.hs.hstesis.iam.domain.model.commands.AddRoleToUserCommand;
import com.hs.hstesis.iam.domain.model.commands.RemoveRoleFromUserCommand;
import com.hs.hstesis.iam.domain.services.UserCommandService;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.RoleRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LearningContextFacade learningContextFacade;

    public UserCommandServiceImpl(UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  LearningContextFacade learningContextFacade) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.learningContextFacade = learningContextFacade;
    }

    @Override
    @Transactional
    public void handle(AddRoleToUserCommand command) {
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        var role = roleRepository.findById(command.roleId())
                .orElseThrow(() -> new RoleNotFoundException(command.roleId()));

        String newRole = role.getRoleName();

        if (newRole.equals("ADMIN")) {
            throw new UnauthorizedRoleAssignmentException("ADMIN");
        }

        boolean isStaffRole = newRole.equals("TEACHER") || newRole.equals("COORDINATOR");
        boolean isStudentRole = newRole.equals("STUDENT");

        if (isStaffRole && user.hasRole("STUDENT")) {
            throw new IncompatibleRoleException(newRole, "STUDENT");
        }
        if (isStudentRole && (user.hasRole("TEACHER") || user.hasRole("COORDINATOR"))) {
            throw new IncompatibleRoleException("STUDENT", "Staff (TEACHER/COORDINATOR)");
        }

        user.addRole(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void handle(RemoveRoleFromUserCommand command) {
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        var role = roleRepository.findById(command.roleId())
                .orElseThrow(() -> new RoleNotFoundException(command.roleId()));

        switch (role.getRoleName()) {
            case "ADMIN" -> {
                if (user.hasRole("ADMIN")) {
                    long currentAdminCount = userRepository.countByRolesId(role.getId());

                    if (currentAdminCount <= 1) {
                        throw new CannotRemoveLastAdminException();
                    }
                }
            }
            case "COORDINATOR" -> {
                if (learningContextFacade.isCoordinatorAssignedToAnyArea(user.getId())) {
                    throw new RoleInUseException("COORDINATOR", "area");
                }
            }
            case "TEACHER" -> {
                if (learningContextFacade.isTeacherAssignedToAnyClassroom(user.getId())) {
                    throw new RoleInUseException("TEACHER", "classroom");
                }
            }
        }

        user.removeRole(role);
        userRepository.save(user);
    }
}
