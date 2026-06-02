package com.hs.hstesis.iam.application.internal.commandservices;

import com.hs.hstesis.iam.application.internal.outboundservices.hashing.HashingService;
import com.hs.hstesis.iam.domain.exceptions.InvalidRoleException;
import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.commands.CreateUserCommand;
import com.hs.hstesis.iam.domain.model.commands.SeedUserRoleCommand;
import com.hs.hstesis.iam.domain.model.entity.Permission;
import com.hs.hstesis.iam.domain.model.entity.Role;
import com.hs.hstesis.iam.domain.model.valueobjects.Permissions;
import com.hs.hstesis.iam.domain.model.valueobjects.Roles;
import com.hs.hstesis.iam.domain.services.UserRoleCommandService;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.PermissionRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.RoleRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserRoleSeedCommandServiceImpl implements UserRoleCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final HashingService hashingService;
    private static final Dotenv dotenv = Dotenv.load();

    public UserRoleSeedCommandServiceImpl(RoleRepository roleRepository,
                                          PermissionRepository permissionRepository,
                                          UserRepository userRepository,
                                          HashingService hashingService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.hashingService = hashingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        handle(new SeedUserRoleCommand());
    }

    @Override
    @Transactional
    public void handle(SeedUserRoleCommand command) {
        Permission classroomRead = createPermissionIfNotFound(Permissions.CLASSROOM_READ);
        Permission classroomMembersRead = createPermissionIfNotFound(Permissions.CLASSROOM_MEMBERS_READ);
        Permission topicsRead = createPermissionIfNotFound(Permissions.TOPICS_READ);
        Permission repositoryRead = createPermissionIfNotFound(Permissions.REPOSITORY_READ);

        createRoleIfNotFound(Roles.ROLE_STUDENT, Set.of(classroomRead, classroomMembersRead, topicsRead, repositoryRead));
        createRoleIfNotFound(Roles.ROLE_TEACHER, Set.of(classroomRead, classroomMembersRead, topicsRead, repositoryRead));
        createRoleIfNotFound(Roles.ROLE_COORDINATOR, Set.of(classroomRead, classroomMembersRead, topicsRead, repositoryRead));
        createRoleIfNotFound(Roles.ROLE_ADMIN, Set.of(classroomRead, classroomMembersRead, topicsRead, repositoryRead));

        String STUDENT_USERNAME = dotenv.get("STUDENT_USERNAME");
        String TEACHER_USERNAME = dotenv.get("TEACHER_USERNAME");
        String COORDINATOR_USERNAME = dotenv.get("COORDINATOR_USERNAME");
        String ADMIN_USERNAME = dotenv.get("ADMIN_USERNAME");
        String COORDINATOR2_USERNAME = dotenv.get("COORDINATOR2_USERNAME");

        String STUDENT_PASSWORD = dotenv.get("STUDENT_PASSWORD");
        String TEACHER_PASSWORD = dotenv.get("TEACHER_PASSWORD");
        String COORDINATOR_PASSWORD = dotenv.get("COORDINATOR_PASSWORD");
        String ADMIN_PASSWORD = dotenv.get("ADMIN_PASSWORD");
        String COORDINATOR2_PASSWORD = dotenv.get("COORDINATOR2_PASSWORD");

        createDefaultUserIfNotFound("Henry", STUDENT_USERNAME, STUDENT_PASSWORD, List.of(Roles.ROLE_STUDENT));
        createDefaultUserIfNotFound("John", TEACHER_USERNAME, TEACHER_PASSWORD, List.of(Roles.ROLE_TEACHER));
        createDefaultUserIfNotFound("Sebastian", COORDINATOR_USERNAME, COORDINATOR_PASSWORD, List.of(Roles.ROLE_TEACHER, Roles.ROLE_COORDINATOR));
        createDefaultUserIfNotFound("Alonso", COORDINATOR2_USERNAME, COORDINATOR2_PASSWORD, List.of(Roles.ROLE_TEACHER, Roles.ROLE_COORDINATOR));
        createDefaultUserIfNotFound("Admin", ADMIN_USERNAME, ADMIN_PASSWORD, List.of(Roles.ROLE_ADMIN));
    }

    private Permission createPermissionIfNotFound(String name) {
        return permissionRepository.findByPermissionName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name)));
    }

    private void createRoleIfNotFound(String name, Set<Permission> permissions) {
        roleRepository.findByRoleName(name).ifPresentOrElse(
                role -> {
                    role.setPermissions(permissions);
                    roleRepository.save(role);
                },
                () -> {
                    Role newRole = new Role(name);
                    newRole.setPermissions(permissions);
                    roleRepository.save(newRole);
                }
        );
    }

    private void createDefaultUserIfNotFound(String name, String username, String rawPassword, List<String> roleNames) {
        if (!userRepository.existsByUsername(username)) {

            String encodedPassword = hashingService.encode(rawPassword);
            var command = new CreateUserCommand(name, username, encodedPassword);
            User user = new User(command);

            roleNames.forEach(roleName -> {
                Role role = roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new InvalidRoleException(roleName, username));
                user.getRoles().add(role);
            });

            userRepository.save(user);
        }
    }
}