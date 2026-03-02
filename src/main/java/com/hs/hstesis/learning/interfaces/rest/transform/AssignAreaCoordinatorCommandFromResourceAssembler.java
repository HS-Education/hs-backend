package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.AssignAreaCoordinatorCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.AssignAreaCoordinatorResource;

public class AssignAreaCoordinatorCommandFromResourceAssembler {
    public static AssignAreaCoordinatorCommand toCommandFromResource(AssignAreaCoordinatorResource resource) {
        return new AssignAreaCoordinatorCommand(resource.teacherId(), resource.areaId());
    }
}
