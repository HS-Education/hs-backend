package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.entities.AreaCoordinator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AreaCoordinatorRepository extends JpaRepository<AreaCoordinator, Long> {
    Optional<AreaCoordinator> findByUserId(Long userId);
    Optional<AreaCoordinator> findByAreaId(Long areaId);
    boolean existsByUserIdAndAreaIdNot(Long userId, Long areaId);
    boolean existsByUserIdAndAreaId(Long userId, Long areaId);
    boolean existsByAreaId(Long areaId);
}