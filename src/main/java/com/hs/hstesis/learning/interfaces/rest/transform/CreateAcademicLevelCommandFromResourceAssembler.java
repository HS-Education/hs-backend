package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.CreateAcademicLevelCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateAcademicLevelResource;

public class CreateAcademicLevelCommandFromResourceAssembler {
    public static CreateAcademicLevelCommand toCommandFromResource(CreateAcademicLevelResource resource) {
        return new CreateAcademicLevelCommand(resource.name());
    }
}
