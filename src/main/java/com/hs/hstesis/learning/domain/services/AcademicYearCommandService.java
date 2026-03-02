package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.ActivateAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.commands.CloseAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.commands.CreateAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAcademicYearCommand;

public interface AcademicYearCommandService {
    Long handle(CreateAcademicYearCommand command);
    void handle(DeleteAcademicYearCommand command);
    void handle(ActivateAcademicYearCommand command);
    void handle(CloseAcademicYearCommand command);
}
