package com.hs.hstesis.learning.domain.model.queries;

public record GetGradingPeriodByIdQuery(Long id) {
    public GetGradingPeriodByIdQuery {
        if (id == null) {
            throw new IllegalArgumentException("Grading Period id cannot be null");
        }
    }
}
