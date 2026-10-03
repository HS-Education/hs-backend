package com.hs.hstesis.iam.infrastructure.tokens.jwt.services;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.commands.CreateUserCommand;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.RefreshTokenRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceImplTest {
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenServiceImpl service = new RefreshTokenServiceImpl(tokens, users);
    private final User user = new User(new CreateUserCommand("Synthetic User", "fixture", "hashed"));

    @Test void repeatedLogoutUsesTheSignInLockAndIdempotentBulkDelete() {
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(user));
        service.deleteByUserId(2L);
        service.deleteByUserId(2L);
        var ordered = inOrder(users, tokens);
        ordered.verify(users).findByIdForUpdate(2L);
        ordered.verify(tokens).deleteByUser(user);
        ordered.verify(users).findByIdForUpdate(2L);
        ordered.verify(tokens).deleteByUser(user);
        verify(users, never()).findById(anyLong());
    }

    @Test void logoutOfARemovedUserIsANoOp() {
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.empty());
        service.deleteByUserId(2L);
        verifyNoInteractions(tokens);
    }

    @Test void signInFlushesDeletionBeforeSavingTheReplacement() {
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(user));
        when(tokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var replacement = service.createRefreshToken(2L);
        var ordered = inOrder(users, tokens);
        ordered.verify(users).findByIdForUpdate(2L);
        ordered.verify(tokens).deleteByUser(user);
        ordered.verify(tokens).flush();
        ordered.verify(tokens).save(replacement);
        assertThat(replacement.getUser()).isSameAs(user);
        assertThat(replacement.getToken()).isNotBlank();
    }

    @Test void databaseFailuresAreNotSilentlyReportedAsSuccessfulLogout() {
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("Synthetic database outage")).when(tokens).deleteByUser(user);
        assertThatThrownBy(() -> service.deleteByUserId(2L)).isInstanceOf(IllegalStateException.class);
    }
}
