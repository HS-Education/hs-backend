package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    boolean existsByCourseIdAndSectionIdAndAcademicYearId(Long courseId, Long sectionId, Long academicYearId);
    List<Classroom> findAllBySectionEducationLevelAndSectionGradeLevelAndAcademicYearId(
            EducationLevel educationLevel,
            GradeLevel gradeLevel,
            Long academicYearId
    );
    boolean existsByUserIdAndClassroomId(Long userId, Long classroomId);
    Optional<Enrollment> findByUserIdAndClassroomId(Long userId, Long classroomId);
    void deleteAllByClassroomId(Long classroomId);
}