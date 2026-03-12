package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.interfaces.rest.resources.AcademicYearResource;

public class AcademicYearResourceFromEntityAssembler {
    public static AcademicYearResource toResourceFromEntity(AcademicYear entity) {
        return new AcademicYearResource(entity.getId(), entity.getYear(), entity.getStatus());
    }
}
