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
                event.getTodo().getTodoId(), event.getUserId(), event.getCompletionMethod(), event.getEventId());
        
        // 여기에 Todo 완료 후 처리 로직 추가
        // 예: 통계 업데이트, 알림 발송 등
    }
    
    @Async
    @EventListener
    public void handleTodoCompletedAsync(TodoCompletedEvent event) {
        log.info("Async processing todo completion: {} - User: {} - EventId: {}", 
                event.getTodo().getTodoId(), event.getUserId(), event.getEventId());
        
        // 비동기 처리 로직
        // 예: 외부 서비스 호출, 이메일 발송 등
    }
} 