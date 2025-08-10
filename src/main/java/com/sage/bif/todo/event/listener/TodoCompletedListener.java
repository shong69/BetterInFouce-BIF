package com.sage.bif.todo.event.listener;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.todo.entity.Todo;
import lombok.Getter;

@Getter
public class TodoCompletedListener extends BaseEvent {

    private final transient Todo todo;
    private final Long bifId;
    private final String completionMethod;

    public TodoCompletedListener(Todo todo, Long bifId, String completionMethod) {
        super();
        this.todo = todo;
        this.bifId = bifId;
        this.completionMethod = completionMethod;
    }

    @Override
    public String getEventType() {
        return "";
    }
}