package com.hs.hstesis.iam.domain.services;

import com.hs.hstesis.iam.domain.model.queries.GetUserNameByIdQuery;

import java.util.Optional;

public interface UserQueryService {
    Optional<String> handle(GetUserNameByIdQuery query);
}
