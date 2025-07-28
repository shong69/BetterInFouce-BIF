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
        
        // 여기에 일기 피드백 요청 처리 로직 추가
        // 예: AI 분석 요청, 피드백 생성 등
    }
    
    @Async
    @EventListener
    public void handleDiaryFeedbackRequestedAsync(DiaryFeedbackRequestedEvent event) {
        log.info("Async processing diary feedback request: {} - User: {} - EventId: {}", 
                event.getDiary().getId(), event.getUserId(), event.getEventId());
        
        // 비동기 처리 로직
        // 예: AI 서비스 호출, 피드백 저장 등
    }
} 