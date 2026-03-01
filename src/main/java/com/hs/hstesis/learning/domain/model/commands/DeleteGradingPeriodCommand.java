package com.hs.hstesis.learning.domain.model.commands;

public record DeleteGradingPeriodCommand(Long id) {
    public DeleteGradingPeriodCommand {
        if (id == null) {
            throw new IllegalArgumentException("Grading Period ID cannot be null");
        }
    }
}
