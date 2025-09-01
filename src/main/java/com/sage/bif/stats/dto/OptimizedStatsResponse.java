package com.sage.bif.stats.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sage.bif.stats.entity.EmotionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptimizedStatsResponse {

    @JsonProperty("id")
    private Long bifId;

    @JsonProperty("nick")
    private String nickname;

    @JsonProperty("join")
    private String joinDate;

    @JsonProperty("total")
    private Integer totalDiaryCount;

    @JsonProperty("code")
    private String connectionCode;

    @JsonProperty("stats")
    private String statisticsText;

    @JsonProperty("advice")
    private String guardianAdviceText;

    @JsonProperty("emotions")
    private List<OptimizedEmotionRatio> emotionRatio;

    @JsonProperty("keywords")
    private List<OptimizedKeywordData> topKeywords;

    @JsonProperty("changes")
    private List<OptimizedMonthlyChange> monthlyChange;

    @JsonProperty("character")
    private OptimizedCharacterInfo characterInfo;

    @JsonProperty("achievement")
    private OptimizedAchievementInfo achievementInfo;

    @JsonProperty("trends")
    private List<OptimizedEmotionTrend> emotionTrends;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptimizedEmotionRatio {
        @JsonProperty("e")
        private EmotionType emotion;

        @JsonProperty("v")
        private Integer value;

        @JsonProperty("p")
        private Double percentage;

        @JsonProperty("emoji")
        private String emoji;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptimizedKeywordData {
        @JsonProperty("k")
        private String keyword;

        @JsonProperty("c")
        private Integer count;

        @JsonProperty("r")
        private Integer rank;

        @JsonProperty("nv")
        private Double normalizedValue;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptimizedMonthlyChange {
        @JsonProperty("e")
        private EmotionType emotion;

        @JsonProperty("v")
        private Integer value;

        @JsonProperty("pv")
        private Integer previousValue;

        @JsonProperty("cp")
        private Double changePercentage;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptimizedCharacterInfo {
        @JsonProperty("n")
        private String name;

        @JsonProperty("m")
        private String message;

        @JsonProperty("emoji")
        private String emoji;

        @JsonProperty("mood")
        private String mood;

        @JsonProperty("a")
        private String advice;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptimizedAchievementInfo {
        @JsonProperty("tp")
        private Integer totalPoints;

        @JsonProperty("cl")
        private Integer currentLevel;

        @JsonProperty("lt")
        private String levelTitle;

        @JsonProperty("ra")
        private List<OptimizedAchievement> recentAchievements;

        @JsonProperty("sc")
        private Integer streakCount;

        @JsonProperty("nm")
        private String nextMilestone;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptimizedAchievement {
        @JsonProperty("n")
        private String name;

        @JsonProperty("d")
        private String description;

        @JsonProperty("p")
        private Integer points;

        @JsonProperty("icon")
        private String icon;

        @JsonProperty("ea")
        private String earnedAt;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptimizedEmotionTrend {
        @JsonProperty("d")
        private String date;

        @JsonProperty("de")
        private EmotionType dominantEmotion;

        @JsonProperty("as")
        private Double averageScore;

        @JsonProperty("t")
        private String trend;

        @JsonProperty("desc")
        private String description;
    }

}
