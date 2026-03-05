package com.hs.hstesis.learning.interfaces.rest.resources;

public record ClassroomResource(Long id,
                                Long courseId,
                                String courseName,
                                Long sectionId,
                                String sectionName,
                                Long academicYearId,
                                String academicYearName,
                                Boolean isActive) {
}
