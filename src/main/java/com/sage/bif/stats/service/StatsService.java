package com.sage.bif.stats.service;

import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;

import java.time.LocalDateTime;

public interface StatsService {

    StatsResponse getMonthlyStats(Long bifId);

    GuardianStatsResponse getGuardianStats(Long bifId);

    void generateMonthlyStats(Long bifId, LocalDateTime yearMonth);

    void generateMonthlyStatsAsync(Long bifId, LocalDateTime yearMonth);

    void updateStatsWithKeywords(Long bifId, String diaryContent);

    void updateRealTimeStats(Long bifId);

    void forceRegenerateStats(Long bifId);
}
