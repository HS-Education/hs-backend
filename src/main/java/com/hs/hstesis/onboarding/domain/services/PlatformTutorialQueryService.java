package com.hs.hstesis.onboarding.domain.services;

import com.hs.hstesis.onboarding.domain.model.aggregates.PlatformTutorial;
import com.hs.hstesis.onboarding.domain.model.queries.GetAllPlatformTutorialsQuery;

import java.util.List;

public interface PlatformTutorialQueryService {
    List<PlatformTutorial> handle(GetAllPlatformTutorialsQuery query);
}

