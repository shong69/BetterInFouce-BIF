package com.sage.bif.stats.scheduler;

import com.sage.bif.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 통계 관련 스케줄 작업을 처리하는 클래스
 * 요구사항: 감정 분석은 매월 1일 자정에 진행한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsScheduler {

    private final StatsService statsService;

        /**
     * 매월 1일 자정에 모든 BIF 사용자의 전월 감정 통계 텍스트를 생성
     * 요구사항: 감정 통계는 매월 1일 자정에 진행한다
     * Cron: 0 0 0 1 * ? (초 분 시 일 월 요일)
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void generateMonthlyEmotionStatisticsForAllUsers() {
        log.info("===== 월별 감정 통계 텍스트 생성 스케줄 시작 =====");

        final LocalDateTime now = LocalDateTime.now();
        final YearMonth previousMonth = YearMonth.from(now.minusMonths(1));
        final int year = previousMonth.getYear();
        final int month = previousMonth.getMonthValue();

        log.info("전월 감정 통계 텍스트 생성 대상: {}년 {}월", year, month);

        try {
            // todo: 실제로는 모든 BIF 사용자 목록을 가져와서 처리해야 함
            // 현재는 사용자 서비스가 완성되지 않아 더미 데이터로 처리
            generateEmotionStatisticsForDummyUsers(year, month);

            log.info("===== 월별 감정 통계 텍스트 생성 스케줄 완료 =====");
        } catch (Exception e) {
            log.error("월별 감정 통계 텍스트 생성 중 오류 발생", e);
        }
    }
    
        /**
     * 더미 사용자들을 위한 감정 통계 텍스트와 보호자 조언 생성 (사용자 서비스 완성 전 임시 처리)
     * 요구사항: 감정 통계 텍스트와 보호자 조언을 매월 1일 자정에 생성
     */
    private void generateEmotionStatisticsForDummyUsers(final int year, final int month) {
        // 더미 BIF 사용자 ID들 (실제로는 DB에서 조회)
        final Long[] dummyBifIds = {1L, 2L, 3L}; // 임시 데이터

        for (Long bifId : dummyBifIds) {
            try {
                log.debug("BIF ID {}의 {}년 {}월 감정 통계 텍스트와 보호자 조언 생성 시작", bifId, year, month);
                // 감정 통계 텍스트와 보호자 조언 생성 (도넛 그래프, 키워드, 월별 변화는 실시간 업데이트)
                statsService.generateMonthlyEmotionStatistics(bifId, year, month);
                log.debug("BIF ID {}의 {}년 {}월 감정 통계 텍스트와 보호자 조언 생성 완료", bifId, year, month);
            } catch (Exception e) {
                log.error("BIF ID {}의 감정 통계 텍스트와 보호자 조언 생성 실패: {}", bifId, e.getMessage(), e);
                // 한 사용자 실패해도 다른 사용자는 계속 처리
            }
        }
    }

}
