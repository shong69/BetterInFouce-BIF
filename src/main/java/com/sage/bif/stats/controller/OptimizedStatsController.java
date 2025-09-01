package com.sage.bif.stats.controller;

import com.sage.bif.stats.dto.OptimizedStatsResponse;
import com.sage.bif.stats.dto.StatsResponse;
import com.sage.bif.stats.service.StatsService;
import com.sage.bif.stats.service.StatsResponseOptimizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v2/stats")
@RequiredArgsConstructor
public class OptimizedStatsController {

    private final StatsService statsService;
    private final StatsResponseOptimizer responseOptimizer;

    @GetMapping("/{bifId}")
    public ResponseEntity<OptimizedStatsResponse> getOptimizedMonthlyStats(@PathVariable Long bifId) {
        try {
            log.info("BIF ID {}의 최적화된 월별 통계 조회 시작", bifId);
            
            final var originalResponse = statsService.getMonthlyStats(bifId);
            final var optimizedResponse = responseOptimizer.optimizeStatsResponse(originalResponse);
            
            log.info("BIF ID {}의 최적화된 월별 통계 조회 완료", bifId);
            
            return ResponseEntity.ok(optimizedResponse);
            
        } catch (Exception e) {
            log.error("BIF ID {}의 최적화된 월별 통계 조회 중 오류 발생", bifId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{bifId}/guardian")
    public ResponseEntity<OptimizedStatsResponse> getOptimizedGuardianStats(@PathVariable Long bifId) {
        try {
            log.info("BIF ID {}의 최적화된 보호자 통계 조회 시작", bifId);
            
            final var originalResponse = statsService.getGuardianStats(bifId);
            
            final var optimizedResponse = OptimizedStatsResponse.builder()
                    .bifId(bifId)
                    .nickname(originalResponse.getBifNickname())
                    .joinDate(originalResponse.getGuardianJoinDate())
                    .guardianAdviceText(originalResponse.getAdvice())
                    .emotionRatio(convertToOptimizedEmotionRatio(originalResponse.getEmotionRatio()))
                    .monthlyChange(convertToOptimizedMonthlyChange(originalResponse.getMonthlyChange()))
                    .build();
            
            log.info("BIF ID {}의 최적화된 보호자 통계 조회 완료", bifId);
            
            return ResponseEntity.ok(optimizedResponse);
            
        } catch (Exception e) {
            log.error("BIF ID {}의 최적화된 보호자 통계 조회 중 오류 발생", bifId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<OptimizedStatsResponse.OptimizedEmotionRatio> convertToOptimizedEmotionRatio(
            List<StatsResponse.EmotionRatio> originalEmotionRatio) {
        if (originalEmotionRatio == null) return List.of();
        
        return originalEmotionRatio.stream()
                .map(ratio -> OptimizedStatsResponse.OptimizedEmotionRatio.builder()
                        .emotion(ratio.getEmotion())
                        .value(ratio.getValue())
                        .percentage(ratio.getPercentage())
                        .emoji(ratio.getEmoji())
                        .build())
                .toList();
    }

    private List<OptimizedStatsResponse.OptimizedMonthlyChange> convertToOptimizedMonthlyChange(
            List<StatsResponse.MonthlyChange> originalChanges) {
        if (originalChanges == null) return List.of();
        
        return originalChanges.stream()
                .map(change -> OptimizedStatsResponse.OptimizedMonthlyChange.builder()
                        .emotion(change.getEmotion())
                        .value(change.getValue())
                        .previousValue(change.getPreviousValue())
                        .changePercentage(change.getChangePercentage())
                        .build())
                .toList();
    }

}
