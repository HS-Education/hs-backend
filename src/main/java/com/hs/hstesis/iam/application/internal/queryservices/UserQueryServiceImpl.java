package com.hs.hstesis.iam.application.internal.queryservices;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.queries.GetAllUsersQuery;
import com.hs.hstesis.iam.domain.model.queries.GetUserByIdQuery;
import com.hs.hstesis.iam.domain.model.queries.GetUserNameByIdQuery;
import com.hs.hstesis.iam.domain.model.queries.GetUsersByIdsQuery;
import com.hs.hstesis.iam.domain.model.valueobjects.Roles;
import com.hs.hstesis.iam.domain.services.UserQueryService;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<String> handle(GetUserNameByIdQuery query) {
        return userRepository.findById(query.userId())
                .map(User::getName);
    }

    @Override
    public List<User> handle(GetAllUsersQuery query) {
        return userRepository.findAllByRoles_RoleNameNot(Roles.ROLE_ADMIN);
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public List<User> handle(GetUsersByIdsQuery query) {
        return userRepository.findAllById(query.userIds());
    }
}
