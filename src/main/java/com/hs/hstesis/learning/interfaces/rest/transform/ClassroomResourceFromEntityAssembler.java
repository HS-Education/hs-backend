package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.interfaces.rest.resources.ClassroomResource;

public class ClassroomResourceFromEntityAssembler {
    public static ClassroomResource toResourceFromEntity(Classroom entity) {
        return new ClassroomResource(
                entity.getId(),
                entity.getCourse().getId(),
                entity.getCourse().getName(),
                SectionResourceFromEntityAssembler.toResourceFromEntity(entity.getSection()),
                entity.getAcademicYear().getId(),
                entity.getAcademicYear().getYear(),
                entity.getAcademicYear().getStatus(),
                entity.getStatus()
        );
    }
}
