package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.application.querymodels.EnrollmentQueryModel;
import com.hs.hstesis.learning.interfaces.rest.resources.EnrollmentResource;

public class EnrollmentResourceFromQueryModelAssembler {
    public static EnrollmentResource toResourceFromQueryModel(EnrollmentQueryModel model) {
        return new EnrollmentResource(
                model.id(),
                model.userId(),
                model.userName(),
                model.roleInClassroom()
        );
    }
}
