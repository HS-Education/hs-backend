package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.EditAreaNameCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateAreaResource;

public class UpdateAreaCommandFromResourceAssembler {
    public static EditAreaNameCommand toCommandFromResource(Long areaId, UpdateAreaResource resource) {
        return new EditAreaNameCommand(areaId, resource.newName());
    }
}
