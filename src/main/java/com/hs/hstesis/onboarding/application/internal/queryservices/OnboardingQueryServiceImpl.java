package com.hs.hstesis.onboarding.application.internal.queryservices;

import com.hs.hstesis.onboarding.domain.model.aggregates.OnboardingProfile;
import com.hs.hstesis.onboarding.domain.model.queries.GetOnboardingStatusQuery;
import com.hs.hstesis.onboarding.domain.services.OnboardingQueryService;
import com.hs.hstesis.onboarding.infrastructure.persistance.jpa.repositories.OnboardingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OnboardingQueryServiceImpl implements OnboardingQueryService {

    private final OnboardingRepository onboardingRepository;

    public OnboardingQueryServiceImpl(OnboardingRepository onboardingRepository) {
        this.onboardingRepository = onboardingRepository;
    }

    @Override
    public Optional<OnboardingProfile> handle(GetOnboardingStatusQuery query) {
        return onboardingRepository.findByUserId(query.userId());
    }
}
