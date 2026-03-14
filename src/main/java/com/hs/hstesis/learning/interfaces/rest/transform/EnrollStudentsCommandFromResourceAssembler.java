package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.commands.EnrollStudentsToAcademicLevelCommand;
import com.hs.hstesis.learning.interfaces.rest.resources.EnrollStudentsResource;

public class EnrollStudentsCommandFromResourceAssembler {
    public static EnrollStudentsToAcademicLevelCommand toCommandFromResource(EnrollStudentsResource resource) {
        return new EnrollStudentsToAcademicLevelCommand(
                resource.studentIds(),
                resource.educationLevel(),
                resource.gradeLevel(),
                resource.academicYearId()
        );
    }
}
