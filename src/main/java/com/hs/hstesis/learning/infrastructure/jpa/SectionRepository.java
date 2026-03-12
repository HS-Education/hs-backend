package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findAllByEducationLevelAndGradeLevel(EducationLevel educationLevel, GradeLevel gradeLevel);
}
