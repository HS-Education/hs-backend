package com.hs.hstesis.iam.interfaces.rest.transform;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.entity.Role;
import com.hs.hstesis.iam.interfaces.rest.resources.AuthenticatedUserResource;

import java.util.stream.Collectors;

public class AuthenticatedUserResourceFromEntityAssembler {
    public static AuthenticatedUserResource toResourceFromEntity(User user) {
        var roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        return new AuthenticatedUserResource(
                user.getId(),
                user.getUsername(),
                roles
        );
    }
}