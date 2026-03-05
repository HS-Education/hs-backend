package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.UnassignTeacherFromClassroomsCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UnassignTeacherResource;

public class UnassignTeacherCommandFromResourceAssembler {
    public static UnassignTeacherFromClassroomsCommand toCommandFromResource(UnassignTeacherResource resource) {
        return new UnassignTeacherFromClassroomsCommand(
                resource.teacherId(),
                resource.classroomIds()
        );
    }
}
