package com.sage.bif.stats.dto;

import com.sage.bif.stats.entity.EmotionType;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record StatsResponse(
        String statisticsText,
        String guardianAdviceText,
        Map<EmotionType, Integer> emotionCounts,
        List<KeywordData> topKeywords,
        Map<EmotionType, Integer> previousMonthEmotionCounts
) {
    @Builder
    public record KeywordData(
            String keyword,
            Integer count,
            Integer rank
    ) {}
}
