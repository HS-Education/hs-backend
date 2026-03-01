package com.hs.hstesis.iam.infrastructure.persistance.jpa;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
