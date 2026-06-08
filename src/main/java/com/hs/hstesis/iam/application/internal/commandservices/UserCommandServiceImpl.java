package com.hs.hstesis.iam.application.internal.commandservices;

import com.hs.hstesis.iam.application.internal.outboundservices.acl.ExternalLearningService;
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
    private final ExternalLearningService externalLearningService;
    private final com.hs.hstesis.iam.application.internal.outboundservices.hashing.HashingService hashingService;

    public UserCommandServiceImpl(UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  ExternalLearningService externalLearningService,
                                  com.hs.hstesis.iam.application.internal.outboundservices.hashing.HashingService hashingService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.externalLearningService = externalLearningService;
        this.hashingService = hashingService;
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
                if (externalLearningService.isCoordinatorAssignedToAnyArea(user.getId())) {
                    throw new RoleInUseException("COORDINATOR", "area");
                }
            }
            case "TEACHER" -> {
                if (externalLearningService.isTeacherAssignedToAnyClassroom(user.getId())) {
                    throw new RoleInUseException("TEACHER", "classroom");
                }
            }
        }

        user.removeRole(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public String handle(com.hs.hstesis.iam.domain.model.commands.SignUpCommand command) {
        if (command.roles() == null || command.roles().isEmpty()) {
            throw new IllegalArgumentException("Roles cannot be empty");
        }

        String primaryRole = command.roles().get(0);
        String prefix = "C"; // Default (but we will overwrite based on role)
        if (primaryRole.equals("ROLE_ADMIN")) prefix = "A";
        else if (primaryRole.equals("ROLE_COORDINATOR")) prefix = "C";
        else if (primaryRole.equals("ROLE_TEACHER")) prefix = "P";
        else if (primaryRole.equals("ROLE_STUDENT")) prefix = "E";

        int year = java.time.Year.now().getValue();
        String username = "";
        boolean unique = false;
        java.util.Random random = new java.util.Random();

        while (!unique) {
            int randomNum = 1000 + random.nextInt(9000); // 4 digits
            username = prefix + year + randomNum;
            if (!userRepository.existsByUsername(username)) {
                unique = true;
            }
        }

        String encodedPassword = hashingService.encode(command.rawPassword());
        var createUserCommand = new com.hs.hstesis.iam.domain.model.commands.CreateUserCommand(command.name(), username, encodedPassword);
        com.hs.hstesis.iam.domain.model.aggregates.User user = new com.hs.hstesis.iam.domain.model.aggregates.User(createUserCommand);

        final String finalUsername = username;
        for (String roleName : command.roles()) {
            com.hs.hstesis.iam.domain.model.entity.Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new InvalidRoleException(roleName, finalUsername));
            user.getRoles().add(role);
        }

        userRepository.save(user);
        return username;
    }
}
