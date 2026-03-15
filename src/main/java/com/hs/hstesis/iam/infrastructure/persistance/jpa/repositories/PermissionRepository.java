package com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.iam.domain.model.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByPermissionName(String permissionName);
}
