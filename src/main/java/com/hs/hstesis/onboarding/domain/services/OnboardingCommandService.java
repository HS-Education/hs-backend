package com.hs.hstesis.onboarding.domain.services;

import com.hs.hstesis.onboarding.domain.model.commands.CompleteOnboardingCommand;

import com.hs.hstesis.onboarding.domain.model.commands.MarkQuizzesOnboardingCompletedCommand;
import com.hs.hstesis.onboarding.domain.model.commands.MarkRepositoryOnboardingCompletedCommand;
import java.util.Optional;

public interface OnboardingCommandService {
    void handle(CompleteOnboardingCommand command);
    void handle(MarkQuizzesOnboardingCompletedCommand command);
    void handle(MarkRepositoryOnboardingCompletedCommand command);
}
