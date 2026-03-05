package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.interfaces.rest.resources.StudyPlanResource;

public class StudyPlanResourceFromEntityAssembler {
    public static StudyPlanResource toResourceFromEntity(StudyPlan entity) {
        return new StudyPlanResource(
                entity.getId(),
                entity.getAcademicLevel().getId(),
                entity.getAcademicLevel().getName(),
                entity.getCourse().getId(),
                entity.getCourse().getName()
        );
    }
}
