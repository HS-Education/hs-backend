package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.interfaces.rest.resources.AreaResource;

public class AreaResourceFromEntityAssembler {
    public static AreaResource toResourceFromEntity(Area entity) {
        String coordinatorName = "Unassigned";

        if (entity.getCoordinator() != null && entity.getCoordinator().getUser() != null) {
            coordinatorName = entity.getCoordinator().getUser().getName();
        }
        return new AreaResource(entity.getId(), entity.getName(), coordinatorName);
    }
}
