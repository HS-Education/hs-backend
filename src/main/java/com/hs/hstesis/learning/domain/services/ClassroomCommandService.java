package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.CreateClassroomCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteClassroomCommand;
import com.hs.hstesis.learning.domain.model.commands.GenerateClassroomsCommand;

public interface ClassroomCommandService {
    int handle(GenerateClassroomsCommand command);
    void handle(DeleteClassroomCommand command);
}
