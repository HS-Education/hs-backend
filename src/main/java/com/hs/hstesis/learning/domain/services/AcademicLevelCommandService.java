package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.CreateAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.EditAcademicLevelNameCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;

import java.util.Optional;

public interface AcademicLevelCommandService {
    Long handle(CreateAcademicLevelCommand command);
    Optional<AcademicLevel> handle(EditAcademicLevelNameCommand command);
    void handle(DeleteAcademicLevelCommand command);
}
