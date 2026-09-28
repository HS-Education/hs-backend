package com.hs.hstesis.onboarding.domain.services;

import com.hs.hstesis.onboarding.domain.model.commands.CreatePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.model.commands.DeletePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.model.commands.UpdatePlatformTutorialCommand;

import java.util.Optional;

public interface PlatformTutorialCommandService {
    Optional<Long> handle(CreatePlatformTutorialCommand command);
    void handle(DeletePlatformTutorialCommand command);
    void handle(UpdatePlatformTutorialCommand command);
}

