package com.hs.hstesis.onboarding.application.internal.commandservices;

import com.hs.hstesis.onboarding.domain.model.aggregates.PlatformTutorial;
import com.hs.hstesis.onboarding.domain.model.commands.CreatePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.model.commands.DeletePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.model.commands.UpdatePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.services.PlatformTutorialCommandService;
import com.hs.hstesis.onboarding.infrastructure.persistence.jpa.repositories.PlatformTutorialRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlatformTutorialCommandServiceImpl implements PlatformTutorialCommandService {

    private final PlatformTutorialRepository platformTutorialRepository;
    private final com.hs.hstesis.onboarding.application.internal.outboundservices.acl.ExternalIamService externalIamService;
    private final com.hs.hstesis.onboarding.application.internal.outboundservices.acl.ExternalNotificationService externalNotificationService;

    public PlatformTutorialCommandServiceImpl(
            PlatformTutorialRepository platformTutorialRepository,
            com.hs.hstesis.onboarding.application.internal.outboundservices.acl.ExternalIamService externalIamService,
            com.hs.hstesis.onboarding.application.internal.outboundservices.acl.ExternalNotificationService externalNotificationService) {
        this.platformTutorialRepository = platformTutorialRepository;
        this.externalIamService = externalIamService;
        this.externalNotificationService = externalNotificationService;
    }

    @Override
    public Optional<Long> handle(CreatePlatformTutorialCommand command) {
        var tutorial = new PlatformTutorial(command.title(), command.description(), command.fileUrl());
        platformTutorialRepository.save(tutorial);
        externalIamService.getActiveNonAdminUserIds().forEach(userId -> {
            if (!userId.equals(command.actorId())) {
                externalNotificationService.sendNewTutorialNotification(userId, tutorial.getTitle());
            }
        });
        return Optional.of(tutorial.getId());
    }

    @Override
    public void handle(DeletePlatformTutorialCommand command) {
        if (!platformTutorialRepository.existsById(command.tutorialId())) {
            throw new IllegalArgumentException("Tutorial not found");
        }
        platformTutorialRepository.deleteById(command.tutorialId());
    }

    @Override
    public void handle(UpdatePlatformTutorialCommand command) {
        var tutorial = platformTutorialRepository.findById(command.tutorialId())
                .orElseThrow(() -> new IllegalArgumentException("Tutorial not found"));
        tutorial.setTitle(command.title());
        tutorial.setDescription(command.description());
        tutorial.setFileUrl(command.fileUrl());
        platformTutorialRepository.save(tutorial);
    }
}

