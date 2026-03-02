package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.EditAcademicLevelNameCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateAcademicLevelResource;

public class UpdateAcademicLevelCommandFromResourceAssembler {
    public static EditAcademicLevelNameCommand toCommandFromResource(Long academicLevelId, UpdateAcademicLevelResource resource) {
        return new EditAcademicLevelNameCommand(academicLevelId, resource.newName());
    }
}
