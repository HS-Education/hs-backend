package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    boolean existsByCourseIdAndSectionIdAndAcademicYearId(Long courseId, Long sectionId, Long academicYearId);
}