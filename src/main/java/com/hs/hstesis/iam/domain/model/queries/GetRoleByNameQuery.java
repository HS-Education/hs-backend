package com.hs.hstesis.iam.domain.model.queries;

public record GetRoleByNameQuery(String name) {

    public GetRoleByNameQuery {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be null or blank");
        }
    }
}