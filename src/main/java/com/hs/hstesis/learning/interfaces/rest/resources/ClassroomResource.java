package com.hs.hstesis.learning.interfaces.rest.resources;

import com.hs.hstesis.learning.domain.model.valueobjects.ClassroomStatus;
import com.hs.hstesis.learning.domain.model.valueobjects.AcademicYearStatus;

public record ClassroomResource(Long id,
                                Long courseId,
                                String courseName,
                                String areaName,
                                SectionResource section,
                                Long academicYearId,
                                Integer academicYearName,
                                AcademicYearStatus academicYearStatus,
                                ClassroomStatus status,
                                String teacherName) {
}
