package com.hs.hstesis.iam.domain.model.queries;

public record GetUserByIdQuery(Long id) {
    public GetUserByIdQuery{
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }
}