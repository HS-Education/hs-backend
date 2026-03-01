package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findAllByUserId(Long userId);
    List<Enrollment> findAllByClassroomId(Long classroomId);
    Optional<Enrollment> findByUserIdAndClassroomId(Long userId, Long classroomId);
    boolean existsByUserIdAndClassroomId(Long userId, Long classroomId);

    @Modifying
    @Transactional
    void deleteAllByClassroomId(Long classroomId);
}
