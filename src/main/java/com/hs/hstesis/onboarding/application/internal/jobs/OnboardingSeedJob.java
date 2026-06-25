package com.hs.hstesis.onboarding.application.internal.jobs;

import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import com.hs.hstesis.onboarding.domain.model.aggregates.OnboardingProfile;
import com.hs.hstesis.onboarding.infrastructure.persistance.jpa.repositories.OnboardingRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds OnboardingProfile records for all existing users who don't have one yet.
 * Sets completed=false so that existing users also go through the onboarding modal.
 */
@Component
public class OnboardingSeedJob {

    private final UserRepository userRepository;
    private final OnboardingRepository onboardingRepository;

    public OnboardingSeedJob(UserRepository userRepository,
                             OnboardingRepository onboardingRepository) {
        this.userRepository = userRepository;
        this.onboardingRepository = onboardingRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedOnboardingProfiles() {
        var allUsers = userRepository.findAll();
        for (var user : allUsers) {
            if (!onboardingRepository.existsByUserId(user.getId())) {
                var profile = new OnboardingProfile(user.getId());
                // completed = false by default — existing users will see the onboarding modal
                onboardingRepository.save(profile);
            }
        }
    }
}
