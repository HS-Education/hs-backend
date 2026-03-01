package com.hs.hstesis.learning.domain.model.queries;

public record GetSectionByIdQuery(Long id) {
    public GetSectionByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
    }
}
