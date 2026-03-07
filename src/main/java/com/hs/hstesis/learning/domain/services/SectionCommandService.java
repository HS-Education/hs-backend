package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.domain.model.commands.CreateSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateSectionCommand;

import java.util.Optional;

public interface SectionCommandService {
    Long handle(CreateSectionCommand command);
    Optional<Section> handle(UpdateSectionCommand command);
    void handle(DeleteSectionCommand command);
}
