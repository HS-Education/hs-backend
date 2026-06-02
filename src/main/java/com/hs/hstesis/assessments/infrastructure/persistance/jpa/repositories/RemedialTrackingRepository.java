package com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.assessments.domain.model.entities.RemedialTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemedialTrackingRepository extends JpaRepository<RemedialTracking, Long> {
    List<RemedialTracking> findAllByCourseIdAndIsResolvedFalse(Long courseId);
}
