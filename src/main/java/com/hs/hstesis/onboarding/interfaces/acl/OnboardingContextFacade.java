package com.hs.hstesis.onboarding.interfaces.acl;

import com.hs.hstesis.onboarding.domain.model.queries.GetOnboardingStatusQuery;
import com.hs.hstesis.onboarding.domain.services.OnboardingQueryService;
import org.springframework.stereotype.Component;

/**
 * Anti-Corruption Layer facade for the Onboarding bounded context.
 * Provides a clean API for other bounded contexts to query onboarding state
 * without depending on internal domain details.
 */
@Component
public class OnboardingContextFacade {

    private final OnboardingQueryService onboardingQueryService;

    public OnboardingContextFacade(OnboardingQueryService onboardingQueryService) {
        this.onboardingQueryService = onboardingQueryService;
    }

    /**
     * Checks if the given user has already completed the onboarding.
     *
     * @param userId the user ID to check
     * @return true if onboarding is completed, false otherwise
     */
    public boolean hasUserCompletedOnboarding(Long userId) {
        if (userId == null) return false;
        return onboardingQueryService.handle(new GetOnboardingStatusQuery(userId))
                .map(profile -> profile.isCompleted())
                .orElse(false);
    }
}
