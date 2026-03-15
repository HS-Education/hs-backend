package com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.learning.domain.model.aggregates.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByAreaId(Long areaId);
    boolean existsByAreaId(Long areaId);
}
