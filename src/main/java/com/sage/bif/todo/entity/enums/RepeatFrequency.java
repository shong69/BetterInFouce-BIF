package com.sage.bif.todo.entity.enums;

import lombok.Getter;

@Getter
public enum RepeatFrequency {

    DAILY("daily", "매일"),
    WEEKLY("weekly", "매주");

    private final String value;
    private final String displayName;

    RepeatFrequency(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

}
