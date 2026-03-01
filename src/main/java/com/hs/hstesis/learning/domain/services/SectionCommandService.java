package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.CreateSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.EditSectionNameCommand;

public interface SectionCommandService {
    void handle(CreateSectionCommand command);
    void handle(EditSectionNameCommand command);
    void handle(DeleteSectionCommand command);
}
