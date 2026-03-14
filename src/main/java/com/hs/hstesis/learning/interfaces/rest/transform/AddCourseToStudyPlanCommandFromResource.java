package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.AddCourseToStudyPlanCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.AddCourseToStudyPlanResource;

public class AddCourseToStudyPlanCommandFromResource {
    public static AddCourseToStudyPlanCommand toCommandFromResource(AddCourseToStudyPlanResource resource) {
        return new AddCourseToStudyPlanCommand(
                resource.educationLevel(),
                resource.gradeLevel(),
                resource.courseId()
        );
    }
}
