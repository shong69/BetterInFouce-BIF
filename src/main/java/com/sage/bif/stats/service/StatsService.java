package com.sage.bif.stats.service;

import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;

public interface StatsService {

    StatsResponse getMonthlyStats(final String username);

    GuardianStatsResponse getGuardianStats(final String username);

    void generateMonthlyStats(final Long bifId, final Integer year, final Integer month);
}