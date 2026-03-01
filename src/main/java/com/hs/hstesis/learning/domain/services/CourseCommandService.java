package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.CreateCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.EditCourseNameCommand;

public interface CourseCommandService {
    void handle(CreateCourseCommand command);
    void handle(EditCourseNameCommand command);
    void handle(DeleteCourseCommand command);
}
