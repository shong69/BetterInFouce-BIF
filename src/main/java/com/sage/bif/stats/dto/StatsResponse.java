package com.sage.bif.stats.dto;

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

    private CharacterInfo characterInfo;
    private AchievementInfo achievementInfo;
    private List<EmotionTrend> emotionTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionRatio {
        private EmotionType emotion;
        private Integer value;
        private String emoji;
        private Double percentage;
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
        private Double normalizedValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterInfo {
        private String name;
        private String message;
        private String emoji;
        private String mood;
        private String advice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementInfo {
        private Integer totalPoints;
        private Integer currentLevel;
        private String levelTitle;
        private List<Achievement> recentAchievements;
        private Integer streakCount;
        private String nextMilestone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Achievement {
        private String name;
        private String description;
        private Integer points;
        private String icon;
        private String earnedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionTrend {
        private String date;
        private EmotionType dominantEmotion;
        private Double averageScore;
        private String trend;
        private String description;
    }
    
}
