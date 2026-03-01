package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findAllByCourseAreaId(Long areaId);
    boolean existsByCourseIdAndSectionIdAndAcademicYearId(Long courseId, Long sectionId, Long academicYearId);
    List<Classroom> findAllBySectionAcademicLevelIdAndAcademicYearId(Long academicLevelId, Long academicYearId);

    @Modifying
    @Query("UPDATE Classroom c SET c.isActive = true WHERE c.academicYear.id = :yearId")
    void activateAllByAcademicYearId(Long yearId);

    @Modifying
    @Query("UPDATE Classroom c SET c.isActive = :status WHERE c.academicYear.id = :yearId")
    void updateIsActiveByAcademicYearId(Long yearId, boolean status);
}