package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.AddTopicCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.AddTopicResource;

public class AddTopicCommandFromResourceAssembler {
    public static AddTopicCommand toCommandFromResource(Long courseId, AddTopicResource resource) {
        return new AddTopicCommand(courseId, resource.gradingPeriodId(), resource.name());
    }
}
