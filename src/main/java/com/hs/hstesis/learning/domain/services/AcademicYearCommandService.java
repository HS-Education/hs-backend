package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.GenerateAcademicYearCommand;

public interface AcademicYearCommandService {
    Long handle(GenerateAcademicYearCommand command);
}
