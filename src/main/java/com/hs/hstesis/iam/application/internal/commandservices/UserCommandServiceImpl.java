package com.hs.hstesis.iam.application.internal.commandservices;

import com.hs.hstesis.iam.domain.exceptions.CannotRemoveLastAdminException;
import com.hs.hstesis.iam.domain.exceptions.RoleNotFoundException;
import com.hs.hstesis.iam.domain.exceptions.UserNotFoundException;
import com.hs.hstesis.iam.domain.model.commands.AddRoleToUserCommand;
import com.hs.hstesis.iam.domain.model.commands.RemoveRoleFromUserCommand;
import com.hs.hstesis.iam.domain.services.UserCommandService;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.RoleRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserCommandServiceImpl(UserRepository userRepository,
                                  RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void handle(AddRoleToUserCommand command) {
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        var role = roleRepository.findById(command.roleId())
                .orElseThrow(() -> new RoleNotFoundException(command.roleId()));

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

        if (role.getRoleName().equals("ADMIN")) {
            if (user.hasRole("ADMIN")) {
                long currentAdminCount = userRepository.countByRolesId(role.getId());

                if (currentAdminCount <= 1) {
                    throw new CannotRemoveLastAdminException();
                }
            }
        }

        user.removeRole(role);
        userRepository.save(user);
    }
}
