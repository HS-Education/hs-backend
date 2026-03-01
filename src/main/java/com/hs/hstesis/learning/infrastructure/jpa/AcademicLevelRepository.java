package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicLevelRepository extends JpaRepository<AcademicLevel, Long> {
    boolean existsByName(String name);
}
