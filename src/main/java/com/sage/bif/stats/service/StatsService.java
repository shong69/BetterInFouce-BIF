package com.sage.bif.stats.service;

import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;

public interface StatsService {

    StatsResponse getMonthlyStats(final Long bifId);

    GuardianStatsResponse getGuardianStats(final Long bifId);

    void generateMonthlyStats(final Long bifId, final Integer year, final Integer month);
    
    void generateMonthlyEmotionStatistics(final Long bifId, final Integer year, final Integer month);
}
