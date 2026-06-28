package com.hs.hstesis.onboarding.application.internal.queryservices;

import com.hs.hstesis.onboarding.domain.model.aggregates.PlatformTutorial;
import com.hs.hstesis.onboarding.domain.model.queries.GetAllPlatformTutorialsQuery;
import com.hs.hstesis.onboarding.domain.services.PlatformTutorialQueryService;
import com.hs.hstesis.onboarding.infrastructure.persistence.jpa.repositories.PlatformTutorialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformTutorialQueryServiceImpl implements PlatformTutorialQueryService {

    private final PlatformTutorialRepository platformTutorialRepository;

    public PlatformTutorialQueryServiceImpl(PlatformTutorialRepository platformTutorialRepository) {
        this.platformTutorialRepository = platformTutorialRepository;
    }

    @Override
    public List<PlatformTutorial> handle(GetAllPlatformTutorialsQuery query) {
        return platformTutorialRepository.findAll();
    }
}

