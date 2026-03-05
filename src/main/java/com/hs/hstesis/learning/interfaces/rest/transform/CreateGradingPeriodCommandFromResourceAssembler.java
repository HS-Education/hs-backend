package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.CreateGradingPeriodCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateGradingPeriodResource;

public class CreateGradingPeriodCommandFromResourceAssembler {
    public static CreateGradingPeriodCommand toCommandFromResource(CreateGradingPeriodResource resource){
        return new CreateGradingPeriodCommand(
                resource.bimester(),
                resource.startDate(),
                resource.endDate(),
                resource.academicYearId()
        );
    }
}
