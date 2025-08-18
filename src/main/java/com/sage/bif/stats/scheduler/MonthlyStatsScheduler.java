package com.sage.bif.stats.scheduler;

import com.sage.bif.stats.service.StatsService;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyStatsScheduler {

    private final StatsService statsService;
    private final BifRepository bifRepository;

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generateMonthlyStatsForAllBifs() {
        log.info("매월 1일 자정 - 모든 BIF 월별 통계 생성 시작");

        try {
            List<Bif> activeBifs = bifRepository.findAll();
            LocalDateTime lastMonth = getLastMonthDateTime();

            for (Bif bif : activeBifs) {
                generateStatsForBif(bif, lastMonth);
            }

            log.info("모든 BIF 월별 통계 생성 완료 - 총 {}개", activeBifs.size());

        } catch (Exception e) {
            log.error("월별 통계 생성 스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private LocalDateTime getLastMonthDateTime() {
        return LocalDateTime.now().minusMonths(1)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);
    }

    private void generateStatsForBif(Bif bif, LocalDateTime lastMonth) {
        try {
            statsService.generateMonthlyStats(bif.getBifId(), lastMonth);
            log.debug("BIF ID {}의 {}년 {}월 통계 생성 완료",
                    bif.getBifId(), lastMonth.getYear(), lastMonth.getMonthValue());
        } catch (Exception e) {
            log.error("BIF ID {}의 {}년 {}월 통계 생성 실패: {}",
                    bif.getBifId(), lastMonth.getYear(), lastMonth.getMonthValue(), e.getMessage());
        }
    }

}
