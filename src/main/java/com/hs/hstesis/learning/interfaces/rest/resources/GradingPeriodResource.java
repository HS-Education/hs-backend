package com.hs.hstesis.learning.interfaces.rest.resources;

import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;
import com.hs.hstesis.learning.domain.model.valueobjects.GradingPeriodStatus;

import java.time.LocalDate;

public record GradingPeriodResource(Long id,
                                    Long academicYearId,
                                    Integer academicYearName,
                                    Bimester bimester,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    GradingPeriodStatus status) {
}
