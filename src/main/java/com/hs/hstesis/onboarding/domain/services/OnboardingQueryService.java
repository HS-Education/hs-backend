package com.hs.hstesis.onboarding.domain.services;

import com.hs.hstesis.onboarding.domain.model.aggregates.OnboardingProfile;
import com.hs.hstesis.onboarding.domain.model.queries.GetOnboardingStatusQuery;

import java.util.Optional;

public interface OnboardingQueryService {
    Optional<OnboardingProfile> handle(GetOnboardingStatusQuery query);
}
