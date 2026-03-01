package com.hs.hstesis.learning.domain.model.queries;

public record GetAreaByIdQuery(Long id) {
    public GetAreaByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}