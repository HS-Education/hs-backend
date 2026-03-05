package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.interfaces.rest.resources.EnrollmentResource;

public class EnrollmentResourceFromEntityAssembler {
    public static EnrollmentResource toResourceFromEntity(Enrollment entity) {
        return new EnrollmentResource(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getName(),
                entity.getClassroom().getId(),
                entity.getRoleInClassroom()
        );
    }
}
