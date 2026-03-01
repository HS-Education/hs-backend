package com.hs.hstesis.iam.domain.model.commands;

public record CreateUserCommand(String name, String username, String passwordHash) {}