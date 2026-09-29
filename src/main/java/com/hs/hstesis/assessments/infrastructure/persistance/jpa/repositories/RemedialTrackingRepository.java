package com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.assessments.domain.model.entities.RemedialTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RemedialTrackingRepository extends JpaRepository<RemedialTracking, Long> {
    List<RemedialTracking> findAllByCourseIdAndIsResolvedFalse(Long courseId);
    List<RemedialTracking> findAllByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<RemedialTracking> findFirstByStudentIdAndCourseIdAndWeakTopicIdAndIsResolvedFalse(
            Long studentId, Long courseId, Long weakTopicId);
}
