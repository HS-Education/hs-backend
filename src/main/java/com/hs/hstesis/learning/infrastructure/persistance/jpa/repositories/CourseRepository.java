package com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByAreaId(Long areaId);
    boolean existsByAreaId(Long areaId);

    @Query("SELECT t FROM Topic t WHERE t.course.id = :courseId ORDER BY t.orderIndex ASC")
    List<Topic> findByCourseIdOrderByOrderIndex(Long courseId);

    @Query("SELECT COUNT(t) > 0 FROM Topic t WHERE t.id = :topicId AND t.course.id = :courseId")
    boolean existsTopicInCourse(Long topicId, Long courseId);

    @Query("SELECT COUNT(c) > 0 FROM Course c WHERE c.id = :courseId AND c.area.coordinatorId = :coordinatorId")
    boolean existsByIdAndCoordinatorId(Long courseId, Long coordinatorId);
}
