package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.CreateAcademicYearCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateAcademicYearResource;

public class CreateAcademicYearCommandFromResourceAssembler {
    public static CreateAcademicYearCommand toCommandFromResource(CreateAcademicYearResource resource) {
        return new CreateAcademicYearCommand(resource.year());
    }
}
