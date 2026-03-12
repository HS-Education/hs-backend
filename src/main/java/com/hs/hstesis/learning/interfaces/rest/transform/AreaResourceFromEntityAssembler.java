package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.interfaces.rest.resources.AreaResource;

public class AreaResourceFromEntityAssembler {
    public static AreaResource toResourceFromEntity(Area entity, String coordinatorName) {
        return new AreaResource(
                entity.getId(),
                entity.getName(),
                coordinatorName
        );
    }
}
