package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.CreateAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.EditAcademicLevelNameCommand;

public interface AcademicLevelCommandService {
    void handle(CreateAcademicLevelCommand command);
    void handle(EditAcademicLevelNameCommand command);
    void handle(DeleteAcademicLevelCommand command);
}
