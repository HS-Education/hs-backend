package com.hs.hstesis.iam.application.internal.commandservices;

import com.hs.hstesis.iam.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.iam.application.internal.outboundservices.hashing.HashingService;
import com.hs.hstesis.iam.domain.exceptions.CannotRemoveLastAdminException;
import com.hs.hstesis.iam.domain.exceptions.IncompatibleRoleException;
import com.hs.hstesis.iam.domain.exceptions.RoleInUseException;
import com.hs.hstesis.iam.domain.exceptions.UnauthorizedRoleAssignmentException;
import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.commands.AddRoleToUserCommand;
import com.hs.hstesis.iam.domain.model.commands.CreateUserCommand;
import com.hs.hstesis.iam.domain.model.commands.RemoveRoleFromUserCommand;
import com.hs.hstesis.iam.domain.model.entity.Role;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.RoleRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserCommandServiceImplTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final ExternalLearningService learning = mock(ExternalLearningService.class);
    private final HashingService hashing = mock(HashingService.class);
    private final UserCommandServiceImpl service = new UserCommandServiceImpl(users, roles, learning, hashing);
    private final User user = new User(new CreateUserCommand("Synthetic", "synthetic", "hashed"));

    @BeforeEach
    void givenUser() {
        ReflectionTestUtils.setField(user, "id", 7L);
        when(users.findById(7L)).thenReturn(Optional.of(user));
    }

    private Role role(long id, String name) {
        Role role = new Role(name);
        role.setId(id);
        when(roles.findById(id)).thenReturn(Optional.of(role));
        return role;
    }

    @Test
    void adminCannotBeGrantedViaRegularRoleCommand() {
        role(1L, "ADMIN");
        assertThatThrownBy(() -> service.handle(new AddRoleToUserCommand(7L, 1L)))
                .isInstanceOf(UnauthorizedRoleAssignmentException.class);
        verify(users, never()).save(any());
    }

    @Test
    void studentAndStaffRolesCannotBeMixedInEitherOrder() {
        user.addRole(new Role("STUDENT"));
        role(2L, "TEACHER");
        assertThatThrownBy(() -> service.handle(new AddRoleToUserCommand(7L, 2L)))
                .isInstanceOf(IncompatibleRoleException.class);
        user.getRoles().clear();
        user.addRole(new Role("COORDINATOR"));
        role(3L, "STUDENT");
        assertThatThrownBy(() -> service.handle(new AddRoleToUserCommand(7L, 3L)))
                .isInstanceOf(IncompatibleRoleException.class);
        verify(users, never()).save(any());
    }

    @Test
    void lastAdminCannotBeRemoved() {
        Role admin = role(1L, "ADMIN");
        user.addRole(admin);
        when(users.countByRolesId(1L)).thenReturn(1L);
        assertThatThrownBy(() -> service.handle(new RemoveRoleFromUserCommand(7L, 1L)))
                .isInstanceOf(CannotRemoveLastAdminException.class);
        assertThat(user.hasRole("ADMIN")).isTrue();
        verify(users, never()).save(any());
    }

    @Test
    void coordinatorWithAssignedAreaCannotLoseRole() {
        Role coordinator = role(2L, "COORDINATOR");
        user.addRole(coordinator);
        when(learning.isCoordinatorAssignedToAnyArea(7L)).thenReturn(true);
        assertThatThrownBy(() -> service.handle(new RemoveRoleFromUserCommand(7L, 2L)))
                .isInstanceOf(RoleInUseException.class);
        assertThat(user.hasRole("COORDINATOR")).isTrue();
        verify(users, never()).save(any());
    }

    @Test
    void unusedTeacherRoleCanBeRemoved() {
        Role teacher = role(3L, "TEACHER");
        user.addRole(teacher);
        service.handle(new RemoveRoleFromUserCommand(7L, 3L));
        assertThat(user.hasRole("TEACHER")).isFalse();
        verify(users).save(user);
    }
}
