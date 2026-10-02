package com.hs.hstesis.onboarding.application.internal.commandservices;

import com.hs.hstesis.iam.domain.exceptions.UserNotFoundException;
import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.commands.CreateUserCommand;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import com.hs.hstesis.onboarding.domain.model.aggregates.OnboardingProfile;
import com.hs.hstesis.onboarding.domain.model.commands.*;
import com.hs.hstesis.onboarding.infrastructure.persistance.jpa.repositories.OnboardingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnboardingCommandServiceImplTest {
    private final OnboardingRepository profiles = mock(OnboardingRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OnboardingCommandServiceImpl service = new OnboardingCommandServiceImpl(profiles, users);

    @BeforeEach void existingUser() {
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(
                new User(new CreateUserCommand("Synthetic Teacher", "fixture", "hashed"))));
        when(profiles.findByUserId(2L)).thenReturn(Optional.empty());
    }

    @Test void newlyRegisteredUserCanCompleteWithoutRestartingTheServer() {
        service.handle(new CompleteOnboardingCommand(2L));
        var saved = savedProfile();
        assertThat(saved.getUserId()).isEqualTo(2L);
        assertThat(saved.isCompleted()).isTrue();
        assertThat(saved.getCompletedAt()).isNotNull();
        assertThat(saved.isQuizzesCompleted()).isFalse();
        assertThat(saved.isRepositoryCompleted()).isFalse();
        var ordered = inOrder(users, profiles);
        ordered.verify(users).findByIdForUpdate(2L);
        ordered.verify(profiles).findByUserId(2L);
        ordered.verify(profiles).save(saved);
    }

    @Test void missingProfileCanCompleteEitherFeatureTutorialIndependently() {
        service.handle(new MarkQuizzesOnboardingCompletedCommand(2L));
        var quizzes = savedProfile();
        assertThat(quizzes.isQuizzesCompleted()).isTrue();
        assertThat(quizzes.isCompleted()).isFalse();
        assertThat(quizzes.isRepositoryCompleted()).isFalse();
        clearInvocations(profiles);
        service.handle(new MarkRepositoryOnboardingCompletedCommand(2L));
        var repository = savedProfile();
        assertThat(repository.isRepositoryCompleted()).isTrue();
        assertThat(repository.isCompleted()).isFalse();
        assertThat(repository.isQuizzesCompleted()).isFalse();
    }

    @Test void existingProgressAndOriginalCompletionTimeArePreservedOnRetry() {
        var profile = new OnboardingProfile(2L);
        profile.markCompleted();
        profile.markQuizzesCompleted();
        var completedAt = profile.getCompletedAt();
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(profile));
        service.handle(new CompleteOnboardingCommand(2L));
        service.handle(new MarkRepositoryOnboardingCompletedCommand(2L));
        assertThat(profile.isCompleted()).isTrue();
        assertThat(profile.isQuizzesCompleted()).isTrue();
        assertThat(profile.isRepositoryCompleted()).isTrue();
        assertThat(profile.getCompletedAt()).isEqualTo(completedAt);
        verify(profiles, times(2)).save(profile);
    }

    @Test void unknownUserCannotCreateAnOrphanProfile() {
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.handle(new CompleteOnboardingCommand(2L)))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(profiles);
    }

    private OnboardingProfile savedProfile() {
        var capture = ArgumentCaptor.forClass(OnboardingProfile.class);
        verify(profiles).save(capture.capture());
        return capture.getValue();
    }
}
