package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.AssignTeacherToClassroomsCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.AssignTeacherResource;

public class AssignTeacherCommandFromResourceAssembler {
    public static AssignTeacherToClassroomsCommand toCommandFromResource(AssignTeacherResource resource) {
        return new AssignTeacherToClassroomsCommand(
                resource.teacherId(),
                resource.classroomIds()
        );
    }
}
