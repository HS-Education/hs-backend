package com.hs.hstesis.iam.domain.services;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.queries.GetAllUsersQuery;
import com.hs.hstesis.iam.domain.model.queries.GetUserByIdQuery;
import com.hs.hstesis.iam.domain.model.queries.GetUserNameByIdQuery;
import com.hs.hstesis.iam.domain.model.queries.GetUsersByIdsQuery;

import java.util.List;
import java.util.Optional;

public interface UserQueryService {
    Optional<String> handle(GetUserNameByIdQuery query);
    Optional<User> handle(GetUserByIdQuery query);
    List<User> handle(GetAllUsersQuery query);
    List<User> handle(GetUsersByIdsQuery query);
}
