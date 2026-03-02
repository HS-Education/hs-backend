package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;
import com.hs.hstesis.learning.interfaces.rest.resources.AcademicLevelResource;

public class AcademicLevelResourceFromEntityAssembler {
    public static AcademicLevelResource toResourceFromEntity(AcademicLevel entity) {
        return new AcademicLevelResource(entity.getId(), entity.getName());
    }
}
