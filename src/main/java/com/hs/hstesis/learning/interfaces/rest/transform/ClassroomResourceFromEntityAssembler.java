package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.interfaces.rest.resources.ClassroomResource;

public class ClassroomResourceFromEntityAssembler {
    public static ClassroomResource toResourceFromEntity(Classroom entity, String teacherName) {
        return new ClassroomResource(
                entity.getId(),
                entity.getCourse().getId(),
                entity.getCourse().getName(),
                entity.getCourse().getArea() != null ? entity.getCourse().getArea().getName() : null,
                SectionResourceFromEntityAssembler.toResourceFromEntity(entity.getSection()),
                entity.getAcademicYear().getId(),
                entity.getAcademicYear().getYear(),
                entity.getAcademicYear().getStatus(),
                entity.getStatus(),
                teacherName
        );
    }
}
