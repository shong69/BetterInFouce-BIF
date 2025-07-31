package com.sage.bif.stats.dto.response;

import com.sage.bif.stats.entity.EmotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {

    private String analysisText;
    
    private List<EmotionRatio> emotionRatio;
    
    private List<KeywordData> topKeywords;
    
    private List<MonthlyChange> monthlyChange;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionRatio {
        private EmotionType emotion;
        private Integer value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyChange {
        private String month;
        private EmotionType emotion;
        private Integer value;
        private Integer previousValue;
        private Double changePercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordData {
        private String keyword;
        private Integer count;
        private Integer rank;
    }
} 
