package com.sage.bif.diary.event.listener;

import com.sage.bif.diary.event.model.DiaryCreatedEvent;
import com.sage.bif.diary.event.model.DiaryUpdatedEvent;
import com.sage.bif.diary.event.model.DiaryDeletedEvent;
import com.sage.bif.stats.event.model.StatsUpdateEvent;
import com.sage.bif.common.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final RedisService redisService;

    @EventListener
    public void handleDiaryCreated(DiaryCreatedEvent event) {
        log.info("Diary created: {} - User: {} - Content length: {}",
                event.getDiaryId(), event.getBifId(), event.getContent().length());

        try {
            invalidateAllDiaryCache(event.getBifId());

            publishStatsEvent(event.getBifId(), "DIARY_CREATED", "일기 생성으로 인한 통계 업데이트");

        } catch (Exception e) {
            log.error("Error in handleDiaryCreated: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleDiaryUpdated(DiaryUpdatedEvent event) {
        log.info("Diary updated: {} - User: {} - Content length: {}",
                event.getDiary().getId(), event.getDiary().getUser().getBifId(), event.getDiary().getContent().length());

        try {
            invalidateAllDiaryCache(event.getDiary().getUser().getBifId());

            if (!event.getPreviousContent().equals(event.getDiary().getContent())) {
                log.info("Diary content changed significantly for diary: {}", event.getDiary().getId());

                publishStatsEvent(event.getDiary().getUser().getBifId(), "DIARY_UPDATED", "일기 수정으로 인한 통계 업데이트");
            }

        } catch (Exception e) {
            log.error("Error in handleDiaryUpdated: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleDiaryDeleted(DiaryDeletedEvent event) {
        log.info("Diary deleted: {} - User: {} - Emotion: {}",
                event.getDiaryId(), event.getUserId(), event.getEmotion());

        try {
            invalidateAllDiaryCache(event.getUserId());

            publishStatsEvent(event.getUserId(), "DIARY_DELETED", "일기 삭제로 인한 통계 업데이트");

        } catch (Exception e) {
            log.error("Error in handleDiaryDeleted: {}", e.getMessage(), e);
        }
    }


    private void invalidateAllDiaryCache(Long userId) {
        try {
            String currentYear = String.valueOf(LocalDate.now().getYear());
            String currentMonth = String.valueOf(LocalDate.now().getMonthValue());
            String monthlyCacheKey = String.format("monthly_summary:%d:%s:%s", userId, currentYear, currentMonth);
            redisService.delete(monthlyCacheKey);

            log.info("Monthly summary cache cleared for user: {} - key: {}", userId, monthlyCacheKey);

        } catch (Exception e) {
            log.error("Failed to invalidate diary cache for user {}: {}", userId, e.getMessage());
        }
    }


    private void publishStatsEvent(Long userId, String updateType, String updateReason) {
        StatsUpdateEvent statsEvent = new StatsUpdateEvent(
                this, userId, "", StatsUpdateEvent.EventType.valueOf(updateType)
        );
        eventPublisher.publishEvent(statsEvent);
        log.info("Stats update event published: User={}, Type={}", userId, updateType);
    }

}
