package com.sage.bif.stats.event.listener;

import com.sage.bif.diary.repository.DiaryRepository;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.stats.event.model.StatsUpdateEvent;
import com.sage.bif.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsUpdateEventListener {

    private final StatsService statsService;
    private final DiaryRepository diaryRepository;

    @EventListener
    @Async
    public void handleStatsUpdate(StatsUpdateEvent event) {
        try {
            log.info("통계 업데이트 이벤트 처리 시작 - BIF ID: {}, 이벤트 타입: {}", 
                    event.getBifId(), event.getEventType());

            switch (event.getEventType()) {
                case DIARY_CREATED:
                    log.info("일기 생성으로 인한 통계 업데이트 - BIF ID: {}", event.getBifId());
                    break;
                    
                case DIARY_UPDATED:
                    log.info("일기 수정으로 인한 통계 업데이트 - BIF ID: {}", event.getBifId());
                    handleDiaryUpdated(event.getBifId());
                    break;
                    
                case DIARY_DELETED:
                    log.info("일기 삭제로 인한 통계 업데이트 - BIF ID: {}", event.getBifId());
                    handleDiaryDeleted(event.getBifId());
                    break;
                    
                default:
                    log.warn("알 수 없는 이벤트 타입: {}", event.getEventType());
            }
            
        } catch (Exception e) {
            log.error("통계 업데이트 이벤트 처리 중 오류 발생 - BIF ID: {}, 이벤트 타입: {}", 
                    event.getBifId(), event.getEventType(), e);
        }
    }

    private void handleDiaryUpdated(Long bifId) {
        try {
            log.info("=== 일기 수정으로 인한 통계 갱신 시작 - BIF ID: {} ===", bifId);
            
            List<Diary> userDiaries = diaryRepository.findByUserId(bifId);
            log.info("BIF ID {}의 전체 일기 개수: {}", bifId, userDiaries.size());
            
            if (!userDiaries.isEmpty()) {
                Diary latestDiary = userDiaries.stream()
                        .filter(diary -> !diary.isDeleted())
                        .max(Comparator.comparing(Diary::getCreatedAt))
                        .orElse(null);
                
                if (latestDiary != null) {
                    log.info("일기 수정으로 인한 통계 갱신 - BIF ID: {}, 일기 ID: {}, 내용 길이: {}, 내용 미리보기: {}", 
                            bifId, latestDiary.getId(), latestDiary.getContent().length(), 
                            latestDiary.getContent().substring(0, Math.min(100, latestDiary.getContent().length())));
                    
                    statsService.updateStatsWithKeywords(bifId, latestDiary.getContent());
                    
                    log.info("일기 수정으로 인한 통계 갱신 완료 - BIF ID: {}", bifId);
                } else {
                    log.warn("BIF ID {}의 유효한 일기를 찾을 수 없음", bifId);
                }
            } else {
                log.warn("BIF ID {}의 일기를 찾을 수 없음", bifId);
            }
            
        } catch (Exception e) {
            log.error("일기 수정 통계 갱신 중 오류 발생 - BIF ID: {}", bifId, e);
        }
    }

    private void handleDiaryDeleted(Long bifId) {
        try {
            log.info("일기 삭제로 인한 통계 갱신 - BIF ID: {}", bifId);
            
            statsService.updateRealTimeStats(bifId);
            
            log.info("일기 삭제로 인한 통계 갱신 완료 - BIF ID: {}", bifId);
            
        } catch (Exception e) {
            log.error("일기 삭제 통계 갱신 중 오류 발생 - BIF ID: {}", bifId, e);
        }
    }

}
