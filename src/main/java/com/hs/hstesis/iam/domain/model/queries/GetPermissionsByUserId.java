package com.hs.hstesis.iam.domain.model.queries;

public record GetPermissionsByUserId(Long id) {

    public GetPermissionsByUserId {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
    }
}
