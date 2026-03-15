package com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    boolean existsByYear(Integer year);
    Optional<AcademicYear> findByYear(Integer year);
}
