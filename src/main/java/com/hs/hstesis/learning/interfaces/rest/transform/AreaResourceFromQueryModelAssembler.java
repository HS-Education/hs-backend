package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.application.querymodels.AreaWithCoordinator;
import com.hs.hstesis.learning.interfaces.rest.resources.AreaResource;

public class AreaResourceFromQueryModelAssembler {
    public static AreaResource toResourceFromQueryModel(AreaWithCoordinator model) {
        return new AreaResource(model.area().getId(), model.area().getName(), model.coordinatorName()
        );
    }
}
