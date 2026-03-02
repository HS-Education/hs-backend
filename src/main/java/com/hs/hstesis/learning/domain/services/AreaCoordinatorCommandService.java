package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.AssignAreaCoordinatorCommand;
import com.hs.hstesis.learning.domain.model.commands.ReassignAreaCoordinatorCommand;

public interface AreaCoordinatorCommandService {
    Long handle(AssignAreaCoordinatorCommand command);
    Long handle(ReassignAreaCoordinatorCommand command);
}
