package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.entities.Area;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Long> {
    boolean existsByName(String name);
}
