package com.sage.bif.diary.event.listener;

import com.sage.bif.diary.event.model.DiaryCreatedEvent;
import com.sage.bif.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryCreatedEventListener {

    private final StatsService statsService;

    @EventListener
    @Async
    public void handleDiaryCreated(DiaryCreatedEvent event) {
        try {
            log.info("일기 생성 이벤트 처리 시작 - BIF ID: {}, 일기 내용 길이: {}", 
                    event.getBifId(), event.getContent().length());
            
            statsService.updateStatsWithKeywords(event.getBifId(), event.getContent());
            
            log.info("일기 생성 이벤트 처리 완료 - BIF ID: {}", event.getBifId());
            
        } catch (Exception e) {
            log.error("일기 생성 이벤트 처리 중 오류 발생 - BIF ID: {}", event.getBifId(), e);
        }
    }
}
