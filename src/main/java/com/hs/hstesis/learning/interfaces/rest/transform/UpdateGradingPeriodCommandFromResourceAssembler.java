package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.UpdateGradingPeriodCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateGradingPeriodResource;

public class UpdateGradingPeriodCommandFromResourceAssembler {
    public static UpdateGradingPeriodCommand toCommandFromResource(
            Long academicYearId,
            Long gradingPeriodId,
            UpdateGradingPeriodResource resource) {

        return new UpdateGradingPeriodCommand(
                academicYearId,
                gradingPeriodId,
                resource.startDate(),
                resource.endDate()
        );
    }
}
