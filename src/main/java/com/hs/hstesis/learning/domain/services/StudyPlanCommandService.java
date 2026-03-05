package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.AddCourseToStudyPlanCommand;
import com.hs.hstesis.learning.domain.model.commands.RemoveCourseFromStudyPlanCommand;

public interface StudyPlanCommandService {
    Long handle(AddCourseToStudyPlanCommand command);
    void handle(RemoveCourseFromStudyPlanCommand command);
}
