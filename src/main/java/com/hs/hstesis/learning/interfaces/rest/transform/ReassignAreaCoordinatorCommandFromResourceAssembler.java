package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.ReassignAreaCoordinatorCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.ReassignAreaCoordinatorResource;

public class ReassignAreaCoordinatorCommandFromResourceAssembler {
    public static ReassignAreaCoordinatorCommand toCommandFromResource(Long areaId, ReassignAreaCoordinatorResource resource) {
        return new ReassignAreaCoordinatorCommand(areaId, resource.newCoordinatorId());
    }
}
