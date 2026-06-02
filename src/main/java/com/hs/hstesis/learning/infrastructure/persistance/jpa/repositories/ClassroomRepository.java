package com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    boolean existsByCourseIdAndSectionIdAndAcademicYearId(Long courseId, Long sectionId, Long academicYearId);
    List<Classroom> findAllBySectionEducationLevelAndSectionGradeLevelAndAcademicYearId(
            EducationLevel educationLevel,
            GradeLevel gradeLevel,
            Long academicYearId
    );
    List<Classroom> findAllByCourseId(Long courseId);
}