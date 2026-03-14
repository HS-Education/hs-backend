package com.hs.hstesis.iam.domain.services;

import com.hs.hstesis.iam.domain.model.commands.AddRoleToUserCommand;
import com.hs.hstesis.iam.domain.model.commands.RemoveRoleFromUserCommand;

public interface UserCommandService {
    void handle(AddRoleToUserCommand command);
    void handle(RemoveRoleFromUserCommand command);
}
