package com.hs.hstesis.iam.domain.services;

import com.hs.hstesis.iam.domain.model.commands.AddRoleToUserCommand;
import com.hs.hstesis.iam.domain.model.commands.RemoveRoleFromUserCommand;

import com.hs.hstesis.iam.domain.model.commands.SignUpCommand;

public interface UserCommandService {
    void handle(AddRoleToUserCommand command);
    void handle(RemoveRoleFromUserCommand command);
    String handle(SignUpCommand command);
}
