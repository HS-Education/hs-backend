package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.UpdateAcademicLevelCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateAcademicLevelResource;

public class UpdateAcademicLevelCommandFromResourceAssembler {
    public static UpdateAcademicLevelCommand toCommandFromResource(Long academicLevelId, UpdateAcademicLevelResource resource) {
        return new UpdateAcademicLevelCommand(academicLevelId, resource.name());
    }
}
