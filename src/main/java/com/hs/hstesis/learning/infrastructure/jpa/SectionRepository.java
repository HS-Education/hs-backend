package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findAllByAcademicLevelId(Long academicLevelId);
    boolean existsByName(String name);
    boolean existsByAcademicLevelId(Long academicLevelId);
}
