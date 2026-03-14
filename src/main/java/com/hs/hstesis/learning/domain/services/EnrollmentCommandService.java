package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.commands.AssignTeacherToClassroomsCommand;
import com.hs.hstesis.learning.domain.model.commands.EnrollStudentsToAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.UnassignTeacherFromClassroomsCommand;
import com.hs.hstesis.learning.domain.model.commands.UnenrollStudentFromClassroomsCommand;

public interface EnrollmentCommandService {
    void handle(EnrollStudentsToAcademicLevelCommand command);
    void handle(AssignTeacherToClassroomsCommand command);
    void handle(UnassignTeacherFromClassroomsCommand command);
    void handle(UnenrollStudentFromClassroomsCommand command);
}
