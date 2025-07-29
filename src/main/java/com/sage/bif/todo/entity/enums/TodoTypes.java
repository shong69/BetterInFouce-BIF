package com.sage.bif.todo.entity.enums;

import lombok.Getter;

@Getter
public enum TodoTypes {
    TASK("task"),
    ROUTINE("routine");

    private final String value;

    TodoTypes(String value) {
        this.value = value;
    }
}
