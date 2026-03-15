package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.application.querymodels.AreaWithCoordinatorQueryModel;
import com.hs.hstesis.learning.interfaces.rest.resources.AreaResource;

public class AreaResourceFromQueryModelAssembler {
    public static AreaResource toResourceFromQueryModel(AreaWithCoordinatorQueryModel model) {
        return new AreaResource(model.area().getId(), model.area().getName(), model.coordinatorName()
        );
    }
}
