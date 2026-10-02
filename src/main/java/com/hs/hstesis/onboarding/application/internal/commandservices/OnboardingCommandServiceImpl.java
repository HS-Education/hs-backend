package com.hs.hstesis.onboarding.application.internal.commandservices;

import com.hs.hstesis.onboarding.domain.model.aggregates.OnboardingProfile;
import com.hs.hstesis.onboarding.domain.model.commands.CompleteOnboardingCommand;
import com.hs.hstesis.onboarding.domain.services.OnboardingCommandService;
import com.hs.hstesis.onboarding.infrastructure.persistance.jpa.repositories.OnboardingRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import com.hs.hstesis.iam.domain.exceptions.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingCommandServiceImpl implements OnboardingCommandService {

    private final OnboardingRepository onboardingRepository;
    private final UserRepository userRepository;

    public OnboardingCommandServiceImpl(OnboardingRepository onboardingRepository, UserRepository userRepository) {
        this.onboardingRepository = onboardingRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void handle(CompleteOnboardingCommand command) {
        var profile = findOrCreateProfile(command.userId());

        profile.markCompleted();
        onboardingRepository.save(profile);
    }

    @Override
    @Transactional
    public void handle(com.hs.hstesis.onboarding.domain.model.commands.MarkQuizzesOnboardingCompletedCommand command) {
        var profile = findOrCreateProfile(command.userId());

        profile.markQuizzesCompleted();
        onboardingRepository.save(profile);
    }

    @Override
    @Transactional
    public void handle(com.hs.hstesis.onboarding.domain.model.commands.MarkRepositoryOnboardingCompletedCommand command) {
        var profile = findOrCreateProfile(command.userId());

        profile.markRepositoryCompleted();
        onboardingRepository.save(profile);
    }

    private OnboardingProfile findOrCreateProfile(Long userId) {
        // New users may be registered after the startup seed. Lock the existing
        // user row before checking the profile so concurrent completions cannot
        // create duplicate profiles or lose flags from another tutorial.
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return onboardingRepository.findByUserId(userId)
                .orElseGet(() -> new OnboardingProfile(userId));
    }
}
