package com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.repo.domain.model.aggregates.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findAllByCourseIdAndUserId(Long courseId, Long userId);
    List<ChatSession> findAllByUserIdOrderByIdAsc(Long userId);
}
