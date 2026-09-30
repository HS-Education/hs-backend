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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
    @Autowired
    private Environment environment;

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

        if (!environment.getProperty("app.seed-demo-users", Boolean.class, true)) {
            // Only an explicitly configured administrator is bootstrapped in Azure.
            String adminName = environment.getRequiredProperty("ADMIN_USERNAME");
            String adminPassword = environment.getRequiredProperty("ADMIN_PASSWORD");
            if (adminPassword.length() < 16) throw new IllegalArgumentException("A strong bootstrap password is required");
            if (!userRepository.existsByUsername(adminName)) {
                createDefaultUserIfNotFound("Admin", adminName, adminPassword, List.of(Roles.ROLE_ADMIN));
            }
            return;
        }

        String STUDENT_USERNAME = environment.getProperty("STUDENT_USERNAME", "student");
        String TEACHER_USERNAME = environment.getProperty("TEACHER_USERNAME", "teacher");
        String COORDINATOR_USERNAME = environment.getProperty("COORDINATOR_USERNAME", "coordinator");
        String ADMIN_USERNAME = environment.getProperty("ADMIN_USERNAME", "admin");
        String COORDINATOR2_USERNAME = environment.getProperty("COORDINATOR2_USERNAME", "coordinator2");

        String STUDENT_PASSWORD = environment.getProperty("STUDENT_PASSWORD", "password");
        String TEACHER_PASSWORD = environment.getProperty("TEACHER_PASSWORD", "password");
        String COORDINATOR_PASSWORD = environment.getProperty("COORDINATOR_PASSWORD", "password");
        String ADMIN_PASSWORD = environment.getProperty("ADMIN_PASSWORD", "password");
        String COORDINATOR2_PASSWORD = environment.getProperty("COORDINATOR2_PASSWORD", "password");

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
            user.setTemporaryPassword(false);

            roleNames.forEach(roleName -> {
                Role role = roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> new InvalidRoleException(roleName, username));
                user.getRoles().add(role);
            });

            userRepository.save(user);
        } else {
            userRepository.findByUsername(username).ifPresent(user -> {
                if (user.isTemporaryPassword()) {
                    user.setTemporaryPassword(false);
                    userRepository.save(user);
                }
            });
        }
    }
}
