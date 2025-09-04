package com.sage.bif.stats.service;

import com.sage.bif.stats.dto.StatsResponse;
import com.sage.bif.stats.dto.OptimizedStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsResponseOptimizer {

    public OptimizedStatsResponse optimizeStatsResponse(StatsResponse originalResponse) {
        try {
            return OptimizedStatsResponse.builder()
                    .bifId(originalResponse.getBifId())
                    .nickname(originalResponse.getNickname())
                    .joinDate(originalResponse.getJoinDate())
                    .totalDiaryCount(originalResponse.getTotalDiaryCount())
                    .connectionCode(originalResponse.getConnectionCode())
                    .statisticsText(originalResponse.getStatisticsText())
                    .guardianAdviceText(originalResponse.getGuardianAdviceText())
                    .emotionRatio(optimizeEmotionRatio(originalResponse.getEmotionRatio()))
                    .topKeywords(optimizeKeywordData(originalResponse.getTopKeywords()))
                    .monthlyChange(optimizeMonthlyChange(originalResponse.getMonthlyChange()))
                    .characterInfo(optimizeCharacterInfo(originalResponse.getCharacterInfo()))
                    .achievementInfo(optimizeAchievementInfo(originalResponse.getAchievementInfo()))
                    .emotionTrends(optimizeEmotionTrends(originalResponse.getEmotionTrends()))
                    .build();
        } catch (Exception e) {
            log.error("통계 응답 최적화 중 오류 발생", e);
            return createDefaultOptimizedResponse(originalResponse.getBifId());
        }
    }

    private List<OptimizedStatsResponse.OptimizedEmotionRatio> optimizeEmotionRatio(
            List<StatsResponse.EmotionRatio> originalEmotionRatio) {
        if (originalEmotionRatio == null) return List.of();
        
        return originalEmotionRatio.stream()
                .map(ratio -> OptimizedStatsResponse.OptimizedEmotionRatio.builder()
                        .emotion(ratio.getEmotion())
                        .value(ratio.getValue())
                        .percentage(ratio.getPercentage())
                        .emoji(ratio.getEmoji())
                        .build())
                .toList();
    }

    private List<OptimizedStatsResponse.OptimizedKeywordData> optimizeKeywordData(
            List<StatsResponse.KeywordData> originalKeywords) {
        if (originalKeywords == null) return List.of();
        
        return originalKeywords.stream()
                .map(keyword -> OptimizedStatsResponse.OptimizedKeywordData.builder()
                        .keyword(keyword.getKeyword())
                        .count(keyword.getCount())
                        .rank(keyword.getRank())
                        .normalizedValue(keyword.getNormalizedValue())
                        .build())
                .toList();
    }

    private List<OptimizedStatsResponse.OptimizedMonthlyChange> optimizeMonthlyChange(
            List<StatsResponse.MonthlyChange> originalChanges) {
        if (originalChanges == null) return List.of();
        
        return originalChanges.stream()
                .map(change -> OptimizedStatsResponse.OptimizedMonthlyChange.builder()
                        .emotion(change.getEmotion())
                        .value(change.getValue())
                        .previousValue(change.getPreviousValue())
                        .changePercentage(change.getChangePercentage())
                        .build())
                .toList();
    }

    private OptimizedStatsResponse.OptimizedCharacterInfo optimizeCharacterInfo(
            StatsResponse.CharacterInfo originalCharacter) {
        if (originalCharacter == null) return null;
        
        return OptimizedStatsResponse.OptimizedCharacterInfo.builder()
                .name(originalCharacter.getName())
                .message(originalCharacter.getMessage())
                .emoji(originalCharacter.getEmoji())
                .mood(originalCharacter.getMood())
                .advice(originalCharacter.getAdvice())
                .build();
    }

    private OptimizedStatsResponse.OptimizedAchievementInfo optimizeAchievementInfo(
            StatsResponse.AchievementInfo originalAchievement) {
        if (originalAchievement == null) return null;
        
        return OptimizedStatsResponse.OptimizedAchievementInfo.builder()
                .totalPoints(originalAchievement.getTotalPoints())
                .currentLevel(originalAchievement.getCurrentLevel())
                .levelTitle(originalAchievement.getLevelTitle())
                .recentAchievements(optimizeAchievements(originalAchievement.getRecentAchievements()))
                .streakCount(originalAchievement.getStreakCount())
                .nextMilestone(originalAchievement.getNextMilestone())
                .build();
    }

    private List<OptimizedStatsResponse.OptimizedAchievement> optimizeAchievements(
            List<StatsResponse.Achievement> originalAchievements) {
        if (originalAchievements == null) return List.of();
        
        return originalAchievements.stream()
                .map(achievement -> OptimizedStatsResponse.OptimizedAchievement.builder()
                        .name(achievement.getName())
                        .description(achievement.getDescription())
                        .points(achievement.getPoints())
                        .icon(achievement.getIcon())
                        .earnedAt(achievement.getEarnedAt())
                        .build())
                .toList();
    }

    private List<OptimizedStatsResponse.OptimizedEmotionTrend> optimizeEmotionTrends(
            List<StatsResponse.EmotionTrend> originalTrends) {
        if (originalTrends == null) return List.of();
        
        return originalTrends.stream()
                .map(trend -> OptimizedStatsResponse.OptimizedEmotionTrend.builder()
                        .date(trend.getDate())
                        .dominantEmotion(trend.getDominantEmotion())
                        .averageScore(trend.getAverageScore())
                        .trend(trend.getTrend())
                        .description(trend.getDescription())
                        .build())
                .toList();
    }

    private OptimizedStatsResponse createDefaultOptimizedResponse(Long bifId) {
        return OptimizedStatsResponse.builder()
                .bifId(bifId)
                .nickname("BIF")
                .joinDate("")
                .totalDiaryCount(0)
                .connectionCode("")
                .statisticsText("데이터를 불러오는 중입니다.")
                .guardianAdviceText("")
                .build();
    }

}
