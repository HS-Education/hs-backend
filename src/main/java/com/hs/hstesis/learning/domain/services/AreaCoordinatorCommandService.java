package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.AssignAreaCoordinatorCommand;
import com.hs.hstesis.learning.domain.model.commands.UnassignAreaCoordinatorCommand;

public interface AreaCoordinatorCommandService {
    void handle(AssignAreaCoordinatorCommand command);
    void handle(UnassignAreaCoordinatorCommand command);
}
