package com.hs.hstesis.learning.domain.model.commands;

public record ReassignAreaCoordinatorCommand(Long newCoordinatorId, Long areaId) {
    public ReassignAreaCoordinatorCommand {
        if (newCoordinatorId == null) {
            throw new IllegalArgumentException("New Coordinator ID cannot be null");
        }
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }
    }
}
