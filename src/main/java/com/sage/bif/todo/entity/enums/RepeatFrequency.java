package com.sage.bif.todo.entity.enums;

import lombok.Getter;

@Getter
public enum RepeatFrequency {

    DAILY("daily"),
    WEEKLY("weekly");

    private final String value;

    RepeatFrequency(String value) {
        this.value = value;
    }

}
