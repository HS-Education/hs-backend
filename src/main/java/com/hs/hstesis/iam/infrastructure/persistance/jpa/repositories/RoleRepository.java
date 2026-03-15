package com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.iam.domain.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);
}
