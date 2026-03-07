package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.CreateAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;

import java.util.Optional;

public interface AcademicLevelCommandService {
    Long handle(CreateAcademicLevelCommand command);
    Optional<AcademicLevel> handle(UpdateAcademicLevelCommand command);
    void handle(DeleteAcademicLevelCommand command);
}
