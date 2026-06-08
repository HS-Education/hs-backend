package com.hs.hstesis.achievements.domain.exceptions;

public class AchievementNotFoundException extends RuntimeException {
    public AchievementNotFoundException(String message) {
        super(message);
    }
}
