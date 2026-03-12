package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.CreateAreaCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateAreaResource;

public class CreateAreaCommandFromResourceAssembler {
    public static CreateAreaCommand toCommandFromResource(CreateAreaResource resource) {
        return new CreateAreaCommand(resource.name(), resource.coordinatorId());
    }
}
