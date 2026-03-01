package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    boolean existsByYear(Integer year);
    Optional<AcademicYear> findByIsActiveTrue();

    @Modifying
    @Query("UPDATE AcademicYear a SET a.isActive = false")
    void deactivateAllYears();
}
