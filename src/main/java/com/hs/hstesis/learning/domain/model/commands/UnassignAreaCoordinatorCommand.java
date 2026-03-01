package com.hs.hstesis.learning.domain.model.commands;

public record UnassignAreaCoordinatorCommand(Long userId, Long areaId) {
    public UnassignAreaCoordinatorCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }
    }
}
