package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.CreateGradingPeriodCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteGradingPeriodCommand;

public interface GradingPeriodCommandService {
    void handle(CreateGradingPeriodCommand command);
    void handle(DeleteGradingPeriodCommand command);
    void refreshPeriodsStatus();
}
