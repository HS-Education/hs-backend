package com.hs.hstesis.iam.domain.model.commands;

public record RemoveRoleFromUserCommand(Long  userId, Long roleId) {
    public RemoveRoleFromUserCommand {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id cannot be null or negative.");
        }
        if (roleId == null || roleId <= 0) {
            throw new IllegalArgumentException("Role id cannot be null or negative.");
        }
    }
}
