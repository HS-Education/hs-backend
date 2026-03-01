package com.hs.hstesis.iam.domain.services;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.queries.GetUserByIdQuery;

import java.util.Optional;

public interface UserQueryService {

    Optional<User> getUserById(GetUserByIdQuery query);
}