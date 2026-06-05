package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.entities.Topic;
import com.hs.hstesis.learning.interfaces.rest.resources.TopicResource;

public class TopicResourceFromEntityAssembler {
    public static TopicResource toResourceFromEntity(Topic entity) {
        return new TopicResource(
                entity.getId(),
                entity.getName(),
                entity.getOrderIndex(),
                entity.getGradingPeriod().getId()
        );
    }
}
