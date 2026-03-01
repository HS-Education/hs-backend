package com.hs.hstesis.iam.domain.services;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.commands.SignInCommand;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Optional;

public interface SignInCommandService {

    Optional<ImmutablePair<User,String>> handle(SignInCommand command);
}
