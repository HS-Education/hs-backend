package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.UnenrollUserCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UnassignUserResource;

public class UnenrollUserCommandFromResourceAssembler {
    public static UnenrollUserCommand toCommandFromResource(UnassignUserResource resource) {
        return new UnenrollUserCommand(resource.userId(), resource.classroomId());
    }
}
