package com.hs.hstesis.learning.domain.exceptions;

public class InvalidEnumValueException extends IllegalArgumentException {
    public InvalidEnumValueException(String invalidValue, String acceptedValues) {
        super(String.format("Invalid value '%s'. Accepted values are: %s", invalidValue, acceptedValues));
    }
}
