package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.UnenrollStudentFromClassroomsCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UnenrollStudentResource;

public class UnenrollStudentCommandFromResourceAssembler {
    public static UnenrollStudentFromClassroomsCommand toCommandFromResource(UnenrollStudentResource resource) {
        return new UnenrollStudentFromClassroomsCommand(resource.userId(), resource.classroomIds());
    }
}
