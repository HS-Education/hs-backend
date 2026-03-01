package com.hs.hstesis.iam.application.internal.commandservices;

import com.hs.hstesis.iam.application.internal.outboundservices.tokens.TokenService;
import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.commands.SignInCommand;
import com.hs.hstesis.iam.domain.services.SignInCommandService;
import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.UserRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SignInCommandServiceImpl implements SignInCommandService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public SignInCommandServiceImpl(UserRepository userRepository, TokenService tokenService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(command.username(), command.password())
        );
        var token = tokenService.generateToken(authentication);
        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        assert userDetails != null;
        var user = userRepository.findById(userDetails.getId());

        return user.map(u -> new ImmutablePair<>(u, token));
    }
}
