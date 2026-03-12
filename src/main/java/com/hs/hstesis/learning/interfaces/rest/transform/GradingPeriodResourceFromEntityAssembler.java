package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.interfaces.rest.resources.GradingPeriodResource;

public class GradingPeriodResourceFromEntityAssembler {
    public static GradingPeriodResource toResourceFromEntity(GradingPeriod entity) {
        return new GradingPeriodResource(
                entity.getId(),
                entity.getAcademicYear().getId(),
                entity.getAcademicYear().getYear(),
                entity.getBimester(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus()
        );
    }
}
