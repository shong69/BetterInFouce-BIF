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

    private String statisticsText;
    private String guardianAdviceText;

    private List<EmotionRatio> emotionRatio;

    private List<KeywordData> topKeywords;

    private List<MonthlyChange> monthlyChange;

    private Long bifId;
    private String nickname;
    private String joinDate;
    private Integer totalDiaryCount;
    private String connectionCode;

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
        private String changeStatus;
        private String changeDescription;
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
