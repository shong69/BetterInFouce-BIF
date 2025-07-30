package com.sage.bif.stats.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianStatsResponse {

    private String bifNickname;
    private String advice;
    private String warning;
    private List<StatsResponse.EmotionRatio> emotionRatio;
    private List<StatsResponse.MonthlyChange> monthlyChange;
} 