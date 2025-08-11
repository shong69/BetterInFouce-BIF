package com.sage.bif.stats.service;

import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;

import java.time.LocalDateTime;

public interface StatsService {

    StatsResponse getMonthlyStats(final Long bifId);

    GuardianStatsResponse getGuardianStats(final Long bifId);

    void generateMonthlyStats(final Long bifId, final LocalDateTime yearMonth);
    
    void generateMonthlyEmotionStatistics(final Long bifId, final LocalDateTime yearMonth);

    void updateRealTimeStats(final Long bifId);
}
