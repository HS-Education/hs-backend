package com.hs.hstesis.iam.application.internal.queryservices;

import com.hs.hstesis.iam.domain.model.entity.Role;
import com.hs.hstesis.iam.domain.model.queries.GetRoleByNameQuery;
import com.hs.hstesis.iam.domain.services.RoleQueryService;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleQueryServiceImpl implements RoleQueryService {

    private final RoleRepository roleRepository;

    public RoleQueryServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<Role> getRoleByName(GetRoleByNameQuery query){
        return roleRepository.findByRoleName(query.name());
    }
}
