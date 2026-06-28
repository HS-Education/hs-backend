package com.hs.hstesis.onboarding.application.internal.commandservices;

import com.hs.hstesis.onboarding.domain.model.aggregates.PlatformTutorial;
import com.hs.hstesis.onboarding.domain.model.commands.CreatePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.model.commands.DeletePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.services.PlatformTutorialCommandService;
import com.hs.hstesis.onboarding.infrastructure.persistence.jpa.repositories.PlatformTutorialRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlatformTutorialCommandServiceImpl implements PlatformTutorialCommandService {

    private final PlatformTutorialRepository platformTutorialRepository;

    public PlatformTutorialCommandServiceImpl(PlatformTutorialRepository platformTutorialRepository) {
        this.platformTutorialRepository = platformTutorialRepository;
    }

    @Override
    public Optional<Long> handle(CreatePlatformTutorialCommand command) {
        var tutorial = new PlatformTutorial(command.title(), command.description(), command.fileUrl());
        platformTutorialRepository.save(tutorial);
        return Optional.of(tutorial.getId());
    }

    @Override
    public void handle(DeletePlatformTutorialCommand command) {
        if (!platformTutorialRepository.existsById(command.tutorialId())) {
            throw new IllegalArgumentException("Tutorial not found");
        }
        platformTutorialRepository.deleteById(command.tutorialId());
    }
}

