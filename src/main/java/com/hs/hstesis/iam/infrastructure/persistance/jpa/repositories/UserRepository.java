package com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAllByRoles_RoleNameNot(String roleName);
    long countByRolesId(Long roleId);
}
