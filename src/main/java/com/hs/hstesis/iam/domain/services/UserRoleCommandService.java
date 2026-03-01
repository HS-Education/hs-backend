package com.hs.hstesis.iam.domain.services;

import com.hs.hstesis.iam.domain.model.commands.SeedUserRoleCommand;

public interface UserRoleCommandService {

    void handle(SeedUserRoleCommand command);
}
