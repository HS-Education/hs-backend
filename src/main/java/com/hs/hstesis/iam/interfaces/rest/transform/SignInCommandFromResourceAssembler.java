package com.hs.hstesis.iam.interfaces.rest.transform;

import com.hs.hstesis.iam.domain.model.commands.SignInCommand;
import com.hs.hstesis.iam.interfaces.rest.resources.SignInResource;

public class SignInCommandFromResourceAssembler {
    public static SignInCommand toCommandFromResource(SignInResource signInResource) {
        return new SignInCommand(signInResource.username(), signInResource.password());
    }
}