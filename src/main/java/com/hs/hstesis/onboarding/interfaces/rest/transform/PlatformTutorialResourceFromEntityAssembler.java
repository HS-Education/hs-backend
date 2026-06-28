package com.hs.hstesis.onboarding.interfaces.rest.transform;

import com.hs.hstesis.onboarding.domain.model.aggregates.PlatformTutorial;
import com.hs.hstesis.onboarding.interfaces.rest.resources.PlatformTutorialResource;

public class PlatformTutorialResourceFromEntityAssembler {
    public static PlatformTutorialResource toResourceFromEntity(PlatformTutorial entity) {
        return new PlatformTutorialResource(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getFileUrl(),
                entity.getCreatedAt()
        );
    }
}

