package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.interfaces.rest.resources.GradingPeriodResource;

public class GradingPeriodResourceFromEntityAssembler {
    public static GradingPeriodResource toResourceFromEntity(GradingPeriod entity) {
        return new GradingPeriodResource(
                entity.getId(),
                entity.getAcademicYear().getId(),
                entity.getAcademicYear().getYear().toString(),
                entity.getBimester(),
                entity.getStartDate().toString(),
                entity.getEndDate().toString(),
                entity.getIsActive()
        );
    }
}
