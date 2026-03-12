package com.hs.hstesis.learning.interfaces.rest.resources;

import com.hs.hstesis.learning.domain.model.valueobjects.ClassroomStatus;

public record ClassroomResource(Long id,
                                Long courseId,
                                String courseName,
                                SectionResource sectionResource,
                                Long academicYearId,
                                Integer academicYearName,
                                ClassroomStatus status) {
}
