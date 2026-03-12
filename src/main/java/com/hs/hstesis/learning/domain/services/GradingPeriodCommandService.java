package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.commands.UpdateGradingPeriodCommand;

import java.util.Optional;

public interface GradingPeriodCommandService {
    Optional<GradingPeriod> handle(UpdateGradingPeriodCommand command);
}
