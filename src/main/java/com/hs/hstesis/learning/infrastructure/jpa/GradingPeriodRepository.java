package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradingPeriodRepository extends JpaRepository<GradingPeriod, Long> {
    List<GradingPeriod> findAllByAcademicYearId(Long academicYearId);
    Optional<GradingPeriod> findByIdAndAcademicYearId(Long id, Long academicYearId);
}
