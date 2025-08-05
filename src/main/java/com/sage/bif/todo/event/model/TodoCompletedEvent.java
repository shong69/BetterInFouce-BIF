package com.sage.bif.todo.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.todo.entity.Todo;
import lombok.Getter;

@Getter
public class TodoCompletedEvent extends BaseEvent {

    private final Todo todo;
    private final Long userId;
    private final String completionMethod;

    public TodoCompletedEvent(Object source, Todo todo, Long userId, String completionMethod) {
        super(source);
        this.todo = todo;
        this.userId = userId;
        this.completionMethod = completionMethod;
    }

    public TodoCompletedEvent(Object source, Todo todo, Long userId, String completionMethod, String correlationId) {
        super(source, correlationId);
        this.todo = todo;
        this.userId = userId;
        this.completionMethod = completionMethod;
    }

    @Override
    public String getEventType() {
        return "TODO_COMPLETED";
    }
}