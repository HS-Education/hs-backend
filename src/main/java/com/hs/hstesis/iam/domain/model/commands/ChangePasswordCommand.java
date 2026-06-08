package com.hs.hstesis.iam.domain.model.commands;

public record ChangePasswordCommand(
        String username,
        String oldPassword,
        String newPassword
) {}
