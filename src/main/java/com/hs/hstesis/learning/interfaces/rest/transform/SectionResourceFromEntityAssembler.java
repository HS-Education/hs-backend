package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.interfaces.rest.resources.SectionResource;

public class SectionResourceFromEntityAssembler {
    public static SectionResource toResourceFromEntity(Section entity) {
        return new SectionResource(
                entity.getId(),
                entity.getName(),
                entity.getAcademicLevel().getName()
        );
    }
}
