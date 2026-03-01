package com.hs.hstesis.iam.application.internal.queryservices;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.queries.GetUserByIdQuery;
import com.hs.hstesis.iam.domain.services.UserQueryService;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> getUserById(GetUserByIdQuery query){

        Long userId = query.id();
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User with ID " + userId + " not found");
        }
        return user;
    }
}