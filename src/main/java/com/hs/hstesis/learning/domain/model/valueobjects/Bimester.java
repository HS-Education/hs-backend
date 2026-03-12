package com.hs.hstesis.learning.domain.model.valueobjects;

public enum Bimester {
    FIRST(1),
    SECOND(2),
    THIRD(3),
    FOURTH(4);

    private final int value;

    Bimester(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
