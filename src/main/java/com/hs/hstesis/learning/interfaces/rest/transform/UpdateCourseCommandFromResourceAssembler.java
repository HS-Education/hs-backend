package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.EditCourseNameCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateCourseResource;

public class UpdateCourseCommandFromResourceAssembler {
    public static EditCourseNameCommand toCommandFromResource(Long areaId, UpdateCourseResource resource) {
        return new EditCourseNameCommand(areaId, resource.newName());
    }
}
