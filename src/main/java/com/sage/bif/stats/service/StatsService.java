package com.sage.bif.stats.service;

import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;

public interface StatsService {

    StatsResponse getMonthlyStats(final String username);

    GuardianStatsResponse getGuardianStats(final String username);

    void generateMonthlyStats(final Long bifId, final Integer year, final Integer month);
    
    /**
     * 감정 분석 텍스트만 생성 (매월 1일 자정 스케줄링용)
     * 요구사항: 감정 분석은 매월 1일 자정에 진행한다
     */
    void generateMonthlyEmotionStatistics(final Long bifId, final Integer year, final Integer month);
}
