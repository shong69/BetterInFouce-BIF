package com.sage.bif.stats.event.listener;

import com.sage.bif.stats.event.model.StatsUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StatsUpdatedListener {
    
    @EventListener
    public void handleStatsUpdated(StatsUpdatedEvent event) {
        log.info("Stats updated: {} - User: {} - Type: {} - Reason: {} - EventId: {}", 
                event.getStats().getId(), event.getUserId(), event.getUpdateType(), 
                event.getUpdateReason(), event.getEventId());
        
        // 여기에 통계 업데이트 처리 로직 추가
        // 예: 캐시 갱신, 알림 발송 등
    }
    
    @Async
    @EventListener
    public void handleStatsUpdatedAsync(StatsUpdatedEvent event) {
        log.info("Async processing stats update: {} - User: {} - Type: {} - EventId: {}", 
                event.getStats().getId(), event.getUserId(), event.getUpdateType(), event.getEventId());
        
        // 비동기 처리 로직
        // 예: 외부 서비스 호출, 데이터 분석 등
    }
} 