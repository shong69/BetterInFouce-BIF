package com.sage.bif.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private static final Map<String, Integer> ACHIEVEMENT_POINTS = Map.of(
            "FIRST_DIARY", 10,
            "DIARY_STREAK_3", 20,
            "DIARY_STREAK_7", 50,
            "DIARY_STREAK_30", 100,
            "EMOTION_VARIETY", 30,
            "KEYWORD_MASTER", 25,
            "GUARDIAN_CONNECTION", 40,
            "MONTHLY_COMPLETE", 60
    );

    private static final Map<Integer, String> LEVEL_TITLES = Map.of(
            1, "감정 탐험가",
            2, "일기 작가",
            3, "감정 마스터",
            4, "마음의 치유사",
            5, "감정의 현자"
    );

    public AchievementResult calculateAchievements(Long bifId, int diaryCount, int streakCount, 
                                                 List<String> emotions, List<String> keywords) {
        try {
            List<Achievement> achievements = new ArrayList<>();
            int totalPoints = 0;

            if (diaryCount == 1) {
                int points = ACHIEVEMENT_POINTS.get("FIRST_DIARY");
                achievements.add(createAchievement("FIRST_DIARY", "첫 번째 일기", 
                    "첫 번째 일기를 작성했습니다!", points));
                totalPoints += points;
            }

            if (streakCount >= 3) {
                int points = ACHIEVEMENT_POINTS.get("DIARY_STREAK_3");
                achievements.add(createAchievement("DIARY_STREAK_3", "3일 연속 기록", 
                    "3일 연속으로 일기를 작성했습니다!", points));
                totalPoints += points;
            }

            if (streakCount >= 7) {
                int points = ACHIEVEMENT_POINTS.get("DIARY_STREAK_7");
                achievements.add(createAchievement("DIARY_STREAK_7", "일주일 연속 기록", 
                    "7일 연속으로 일기를 작성했습니다!", points));
                totalPoints += points;
            }

            if (streakCount >= 30) {
                int points = ACHIEVEMENT_POINTS.get("DIARY_STREAK_30");
                achievements.add(createAchievement("DIARY_STREAK_30", "한 달 연속 기록", 
                    "30일 연속으로 일기를 작성했습니다!", points));
                totalPoints += points;
            }

            if (emotions.size() >= 4) {
                int points = ACHIEVEMENT_POINTS.get("EMOTION_VARIETY");
                achievements.add(createAchievement("EMOTION_VARIETY", "감정 다양성", 
                    "다양한 감정을 표현했습니다!", points));
                totalPoints += points;
            }

            if (keywords.size() >= 5) {
                int points = ACHIEVEMENT_POINTS.get("KEYWORD_MASTER");
                achievements.add(createAchievement("KEYWORD_MASTER", "키워드 마스터", 
                    "많은 키워드를 사용했습니다!", points));
                totalPoints += points;
            }

            int currentLevel = calculateLevel(totalPoints);
            String levelTitle = LEVEL_TITLES.getOrDefault(currentLevel, "감정 탐험가");
            String nextMilestone = calculateNextMilestone(totalPoints);

            return AchievementResult.builder()
                    .totalPoints(totalPoints)
                    .currentLevel(currentLevel)
                    .levelTitle(levelTitle)
                    .recentAchievements(achievements)
                    .streakCount(streakCount)
                    .nextMilestone(nextMilestone)
                    .build();

        } catch (Exception e) {
            log.error("업적 계산 중 오류 발생 - bifId: {}", bifId, e);
            return createDefaultAchievementResult();
        }
    }

    private Achievement createAchievement(String code, String name, String description, int points) {
        return Achievement.builder()
                .name(name)
                .description(description)
                .points(points)
                .icon(getAchievementIcon(code))
                .earnedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .build();
    }

    private String getAchievementIcon(String code) {
        return switch (code) {
            case "FIRST_DIARY" -> "📝";
            case "DIARY_STREAK_3" -> "🔥";
            case "DIARY_STREAK_7" -> "🌟";
            case "DIARY_STREAK_30" -> "🏆";
            case "EMOTION_VARIETY" -> "🌈";
            case "KEYWORD_MASTER" -> "🔑";
            case "GUARDIAN_CONNECTION" -> "👥";
            case "MONTHLY_COMPLETE" -> "📅";
            default -> "🎯";
        };
    }

    private int calculateLevel(int totalPoints) {
        if (totalPoints >= 500) return 5;
        if (totalPoints >= 300) return 4;
        if (totalPoints >= 150) return 3;
        if (totalPoints >= 50) return 2;
        return 1;
    }

    private String calculateNextMilestone(int totalPoints) {
        if (totalPoints < 50) return "50점 달성하여 레벨 2 달성";
        if (totalPoints < 150) return "150점 달성하여 레벨 3 달성";
        if (totalPoints < 300) return "300점 달성하여 레벨 4 달성";
        if (totalPoints < 500) return "500점 달성하여 레벨 5 달성";
        return "모든 레벨을 달성했습니다! 🎉";
    }

    private AchievementResult createDefaultAchievementResult() {
        return AchievementResult.builder()
                .totalPoints(0)
                .currentLevel(1)
                .levelTitle("감정 탐험가")
                .recentAchievements(new ArrayList<>())
                .streakCount(0)
                .nextMilestone("10점 달성하여 첫 업적 획득")
                .build();
    }

    public static class AchievementResult {
        private int totalPoints;
        private int currentLevel;
        private String levelTitle;
        private List<Achievement> recentAchievements;
        private int streakCount;
        private String nextMilestone;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AchievementResult result = new AchievementResult();

            public Builder totalPoints(int totalPoints) {
                result.totalPoints = totalPoints;
                return this;
            }

            public Builder currentLevel(int currentLevel) {
                result.currentLevel = currentLevel;
                return this;
            }

            public Builder levelTitle(String levelTitle) {
                result.levelTitle = levelTitle;
                return this;
            }

            public Builder recentAchievements(List<Achievement> recentAchievements) {
                result.recentAchievements = recentAchievements;
                return this;
            }

            public Builder streakCount(int streakCount) {
                result.streakCount = streakCount;
                return this;
            }

            public Builder nextMilestone(String nextMilestone) {
                result.nextMilestone = nextMilestone;
                return this;
            }

            public AchievementResult build() {
                return result;
            }
        }

        public int getTotalPoints() { return totalPoints; }
        public int getCurrentLevel() { return currentLevel; }
        public String getLevelTitle() { return levelTitle; }
        public List<Achievement> getRecentAchievements() { return recentAchievements; }
        public int getStreakCount() { return streakCount; }
        public String getNextMilestone() { return nextMilestone; }
    }

    public static class Achievement {
        private String name;
        private String description;
        private int points;
        private String icon;
        private String earnedAt;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Achievement achievement = new Achievement();

            public Builder name(String name) {
                achievement.name = name;
                return this;
            }

            public Builder description(String description) {
                achievement.description = description;
                return this;
            }

            public Builder points(int points) {
                achievement.points = points;
                return this;
            }

            public Builder icon(String icon) {
                achievement.icon = icon;
                return this;
            }

            public Builder earnedAt(String earnedAt) {
                achievement.earnedAt = earnedAt;
                return this;
            }

            public Achievement build() {
                return achievement;
            }
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getPoints() { return points; }
        public String getIcon() { return icon; }
        public String getEarnedAt() { return earnedAt; }
    }
}
