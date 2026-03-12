package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.CreateSectionCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateSectionResource;

public class CreateSectionCommandFromResourceAssembler {
    public static CreateSectionCommand toCommandFromResource(CreateSectionResource resource){
        return new CreateSectionCommand(
                resource.name(),
                resource.educationLevel(),
                resource.gradeLevel()
        );
    }
}
