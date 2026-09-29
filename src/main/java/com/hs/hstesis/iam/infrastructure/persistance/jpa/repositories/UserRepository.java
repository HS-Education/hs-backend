package com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAllByRoles_RoleNameNot(String roleName);
    long countByRolesId(Long roleId);
}
