package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.CreateAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.EditAreaNameCommand;
import com.hs.hstesis.learning.domain.model.entities.Area;

import java.util.Optional;

public interface AreaCommandService {
    Long handle(CreateAreaCommand command);
    Optional<Area> handle(EditAreaNameCommand command);
    void handle(DeleteAreaCommand command);
}
