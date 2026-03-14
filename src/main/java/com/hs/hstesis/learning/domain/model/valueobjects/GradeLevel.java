package com.hs.hstesis.learning.domain.model.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hs.hstesis.learning.domain.exceptions.InvalidEnumValueException;

import java.util.Arrays;

public enum GradeLevel {
    SECOND;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static GradeLevel fromString(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return GradeLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidEnumValueException(value, Arrays.toString(values()));
        }
    }
}
