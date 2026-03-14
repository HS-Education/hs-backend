package com.hs.hstesis.iam.domain.model.queries;

import java.util.Set;

public record GetUsersByIdsQuery(Set<Long> userIds) {
    public GetUsersByIdsQuery {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("User ids cannot be null or empty");
        }
    }
}
