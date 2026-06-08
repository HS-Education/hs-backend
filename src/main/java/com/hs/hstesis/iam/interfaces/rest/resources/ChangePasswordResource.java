package com.hs.hstesis.iam.interfaces.rest.resources;

public record ChangePasswordResource(
        String username,
        String oldPassword,
        String newPassword
) {}
