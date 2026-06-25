package com.hs.hstesis.onboarding.domain.services;

import com.hs.hstesis.onboarding.domain.model.commands.CompleteOnboardingCommand;

public interface OnboardingCommandService {
    void handle(CompleteOnboardingCommand command);
}
