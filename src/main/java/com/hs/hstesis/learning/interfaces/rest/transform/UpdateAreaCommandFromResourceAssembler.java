package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.UpdateAreaCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateAreaResource;

public class UpdateAreaCommandFromResourceAssembler {
    public static UpdateAreaCommand toCommandFromResource(Long areaId, UpdateAreaResource resource) {
        return new UpdateAreaCommand(areaId, resource.name(), resource.coordinatorId());
    }
}
