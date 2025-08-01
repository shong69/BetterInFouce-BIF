package com.sage.bif.todo.entity.enums;

import lombok.Getter;

@Getter
public enum RepeatDays {

    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    private final int value;

    RepeatDays(int value) {
        this.value = value;
    }

}
