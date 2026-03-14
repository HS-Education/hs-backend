package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

import java.util.List;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(Long userId) {
        super(String.format("User with id %d not found.", userId));
    }

    public UserNotFoundException(List<Long> ids) {
        super("Users", ids);
    }
}
