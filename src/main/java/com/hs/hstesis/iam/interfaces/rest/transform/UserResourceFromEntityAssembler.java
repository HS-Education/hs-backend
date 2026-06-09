package com.hs.hstesis.iam.interfaces.rest.transform;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.entity.Role;
import com.hs.hstesis.iam.interfaces.rest.resources.UserResource;

import java.util.stream.Collectors;

public class UserResourceFromEntityAssembler {
    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.isActive(),
                user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList())
        );
    }
}
