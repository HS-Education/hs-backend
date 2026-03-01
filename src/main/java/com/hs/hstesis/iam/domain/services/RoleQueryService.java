package com.hs.hstesis.iam.domain.services;

import com.hs.hstesis.iam.domain.model.entity.Role;
import com.hs.hstesis.iam.domain.model.queries.GetRoleByNameQuery;

import java.util.Optional;

public interface RoleQueryService {
    Optional<Role> getRoleByName(GetRoleByNameQuery query);
}
