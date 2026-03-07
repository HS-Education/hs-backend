package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.UpdateSectionCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateSectionResource;

public class UpdateSectionCommandFromResourceAssembler {
    public static UpdateSectionCommand toCommandFromResource(Long sectionId, UpdateSectionResource resource) {
        return new UpdateSectionCommand(sectionId, resource.name());
    }
}
