package com.sage.bif.todo.entity.enums;

import lombok.Getter;

@Getter
public enum TodoTypes {

    TASK("task", "할 일"),
    ROUTINE("routine", "루틴");

    private final String value;
    private final String displayName;

    TodoTypes(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

}
