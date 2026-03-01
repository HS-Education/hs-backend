package com.hs.hstesis.iam.domain.model.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public final class UserCreatedEvent extends ApplicationEvent {

    private final Long userId;

    public UserCreatedEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }
}