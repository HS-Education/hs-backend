package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class TopicNotFoundException extends ResourceNotFoundException {
    public TopicNotFoundException(Long id) {
        super("Topic", id);
    }
}
