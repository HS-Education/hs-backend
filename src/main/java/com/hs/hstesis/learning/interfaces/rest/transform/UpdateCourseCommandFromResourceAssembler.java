package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.UpdateCourseCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateCourseResource;

public class UpdateCourseCommandFromResourceAssembler {
    public static UpdateCourseCommand toCommandFromResource(Long areaId, UpdateCourseResource resource) {
        return new UpdateCourseCommand(areaId, resource.name());
    }
}
