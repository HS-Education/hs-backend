package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.ReorderTopicsCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.ReorderTopicsResource;

public class ReorderTopicCommandFromResourceAssembler {
    public static ReorderTopicsCommand toCommandFromResource(Long courseId, ReorderTopicsResource resource) {
        return new ReorderTopicsCommand(courseId, resource.topicIdsInOrder());
    }
}
