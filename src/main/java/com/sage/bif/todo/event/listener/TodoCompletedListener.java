package com.sage.bif.todo.event.listener;

import com.sage.bif.todo.event.model.TodoCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TodoCompletedListener {
    
    @EventListener
    public void handleTodoCompleted(TodoCompletedEvent event) {
        log.info("Todo completed: {} - User: {} - Method: {} - EventId: {}", 
                event.getTodo().getId(), event.getUserId(), event.getCompletionMethod(), event.getEventId());
    }
    
    @Async
    @EventListener
    public void handleTodoCompletedAsync(TodoCompletedEvent event) {
        log.info("Async processing todo completion: {} - User: {} - EventId: {}", 
                event.getTodo().getId(), event.getUserId(), event.getEventId());
    }
} 