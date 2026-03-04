package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.EditSectionNameCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateSectionResource;

public class UpdateSectionCommandFromResourceAssembler {
    public static EditSectionNameCommand toCommandFromResource(Long sectionId, UpdateSectionResource resource) {
        return new EditSectionNameCommand(sectionId, resource.newName());
    }
}
