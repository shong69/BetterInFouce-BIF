package com.sage.bif.diary.event.listener;

import com.sage.bif.diary.event.model.DiaryFeedbackRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DiaryFeedbackListener {
    
    @EventListener
    public void handleDiaryFeedbackRequested(DiaryFeedbackRequestedEvent event) {
        log.info("Diary feedback requested: {} - User: {} - Type: {} - Reason: {} - EventId: {}", 
                event.getDiary().getId(), event.getUserId(), event.getFeedbackType(), 
                event.getRequestReason(), event.getEventId());
    }
    
    @Async
    @EventListener
    public void handleDiaryFeedbackRequestedAsync(DiaryFeedbackRequestedEvent event) {
        log.info("Async processing diary feedback request: {} - User: {} - EventId: {}", 
                event.getDiary().getId(), event.getUserId(), event.getEventId());
    }
} 