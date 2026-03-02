package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.entities.AreaCoordinator;
import com.hs.hstesis.learning.interfaces.rest.resources.AreaCoordinatorResource;

public class AreaCoordinatorResourceFromEntityAssembler {
    public static AreaCoordinatorResource toResourceFromEntity(AreaCoordinator entity) {
        return new AreaCoordinatorResource(entity.getId(), entity.getUser().getName(), entity.getArea().getName());
    }
}
