package com.hs.hstesis.onboarding.application.internal.commandservices;

import com.hs.hstesis.onboarding.domain.model.aggregates.OnboardingProfile;
import com.hs.hstesis.onboarding.domain.model.commands.CompleteOnboardingCommand;
import com.hs.hstesis.onboarding.domain.services.OnboardingCommandService;
import com.hs.hstesis.onboarding.infrastructure.persistance.jpa.repositories.OnboardingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingCommandServiceImpl implements OnboardingCommandService {

    private final OnboardingRepository onboardingRepository;

    public OnboardingCommandServiceImpl(OnboardingRepository onboardingRepository) {
        this.onboardingRepository = onboardingRepository;
    }

    @Override
    @Transactional
    public void handle(CompleteOnboardingCommand command) {
        var profile = onboardingRepository.findByUserId(command.userId())
                .orElseGet(() -> new OnboardingProfile(command.userId()));
        profile.markCompleted();
        onboardingRepository.save(profile);
    }
}
