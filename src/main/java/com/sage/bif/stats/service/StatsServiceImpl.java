package com.sage.bif.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.entity.EmotionType;
import com.sage.bif.stats.entity.Stats;
import com.sage.bif.stats.repository.StatsRepository;
import com.sage.bif.diary.repository.DiaryRepository;
import com.sage.bif.diary.model.Emotion;
import com.sage.bif.stats.util.EmotionMapper;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.user.entity.Bif;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StatsServiceImpl implements StatsService {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String NO_ADVICE_MSG = "BIF의 감정 데이터가 없어 조언을 제공할 수 없습니다.";

    private final StatsRepository statsRepository;
    private final DiaryRepository diaryRepository;
    private final BifRepository bifRepository;
    private final AiEmotionAnalysisService aiEmotionAnalysisService;
    private final AchievementService achievementService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public StatsResponse getMonthlyStats(final Long bifId) {
        try {
            log.info("BIF ID {}의 월별 통계 조회 시작", bifId);
            
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);

            if (existingStats.isEmpty()) {
                log.info("BIF ID {}의 통계 데이터가 없어 새로 생성합니다.", bifId);
                return generateAndSaveMonthlyStats(bifId, currentYearMonth);
            }

            return buildStatsResponseWithRealTimeData(existingStats.get(), bifId, currentYearMonth);
            
        } catch (Exception e) {
            log.error("월별 통계 조회 중 오류 발생 - bifId: {}", bifId, e);
            return createEmptyStatsResponse(bifId);
        }
    }

    @Override
    @Transactional
    public GuardianStatsResponse getGuardianStats(final Long bifId) {
        log.info("보호자가 BIF ID {}의 통계를 조회합니다.", bifId);

        try {
            final StatsResponse bifStats = getMonthlyStats(bifId);
            final String bifNickname = getBifNickname(bifId);
            final String advice = bifStats.getGuardianAdviceText();
            final String guardianJoinDate = getGuardianJoinDateByBifId(bifId);

            return GuardianStatsResponse.builder()
                    .bifNickname(bifNickname)
                    .advice(advice)
                    .guardianJoinDate(guardianJoinDate)
                    .emotionRatio(bifStats.getEmotionRatio())
                    .monthlyChange(bifStats.getMonthlyChange())
                    .build();

        } catch (Exception e) {
            log.error("보호자 통계 조회 중 오류 발생 - BIF ID: {}", bifId, e);
            throw new RuntimeException("보호자 통계 조회 실패", e);
        }
    }

    @Override
    @Transactional
    public void generateMonthlyStats(final Long bifId, final LocalDateTime yearMonth) {
        log.info("BIF ID {}의 {}년 {}월 통계 생성 시작", bifId, yearMonth.getYear(), yearMonth.getMonthValue());
        
        try {
            generateAndSaveMonthlyStats(bifId, yearMonth);
            log.info("BIF ID {}의 {}년 {}월 통계 생성 완료", bifId, yearMonth.getYear(), yearMonth.getMonthValue());
        } catch (Exception e) {
            log.error("BIF ID {}의 {}년 {}월 통계 생성 중 오류 발생", bifId, yearMonth.getYear(), yearMonth.getMonthValue(), e);
        }
    }

    @Override
    public void updateStatsWithKeywords(final Long bifId, final String diaryContent) {
        log.info("BIF ID {}의 키워드 기반 통계 업데이트 시작", bifId);
        
        try {
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            if (existingStats.isPresent()) {
                final Stats stats = existingStats.get();
                final AiEmotionAnalysisService.EmotionAnalysisResult analysis = aiEmotionAnalysisService.analyzeEmotionFromText(diaryContent);
                
                stats.setAiEmotionScore(analysis.getEmotionScore());
                stats.setCharacterName(analysis.getCharacterName());
                stats.setCharacterMessage(analysis.getCharacterMessage());
                
                statsRepository.save(stats);
                log.info("BIF ID {}의 AI 감정 분석 결과 업데이트 완료", bifId);
            }
        } catch (Exception e) {
            log.error("키워드 기반 통계 업데이트 중 오류 발생 - bifId: {}", bifId, e);
        }
    }

    @Override
    @Transactional
    public void updateRealTimeStats(final Long bifId) {
        log.info("BIF ID {}의 실시간 통계 갱신 시작", bifId);
        
        try {
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, currentYearMonth);
            saveEmotionCountsToStats(bifId, currentYearMonth, emotionCounts);
            log.debug("BIF ID {}의 실시간 통계 갱신 완료", bifId);
        } catch (Exception e) {
            log.error("실시간 통계 갱신 중 오류 발생 - bifId: {}", bifId, e);
        }
    }

    @Override
    public void generateMonthlyStatsAsync(final Long bifId, final LocalDateTime yearMonth) {
        try {
            generateMonthlyStats(bifId, yearMonth);
        } catch (Exception e) {
            log.error("비동기 통계 생성 중 오류 발생 - bifId: {}, yearMonth: {}", bifId, yearMonth, e);
        }
    }

    private StatsResponse generateAndSaveMonthlyStats(Long bifId, LocalDateTime yearMonth) {
        try {
            final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, yearMonth);
            
            final List<String> keywords = extractKeywordsFromDiaries(bifId, yearMonth);
            final int streakCount = calculateStreakCount(bifId);
            
            final AiEmotionAnalysisService.EmotionAnalysisResult aiAnalysis = 
                    aiEmotionAnalysisService.analyzeEmotionFromText(""); // 빈 텍스트로 기본 분석
            
            final Stats stats = Stats.builder()
                    .bifId(bifId)
                    .yearMonth(yearMonth)
                    .emotionStatisticsText(generateStatisticsText(emotionCounts))
                    .guardianAdviceText(generateGuardianAdvice(emotionCounts))
                    .emotionCounts(objectMapper.writeValueAsString(emotionCounts))
                    .topKeywords(objectMapper.writeValueAsString(keywords))
                    .aiEmotionScore(aiAnalysis.getEmotionScore())
                    .characterName(aiAnalysis.getCharacterName())
                    .characterMessage(aiAnalysis.getCharacterMessage())
                    .achievementPoints(0)
                    .streakCount(streakCount)
                    .build();

            final Stats savedStats = statsRepository.save(stats);
            log.info("BIF ID {}의 통계 데이터 저장 완료", bifId);

            return buildStatsResponseWithRealTimeData(savedStats, bifId, yearMonth);
            
        } catch (Exception e) {
            log.error("통계 데이터 생성 및 저장 중 오류 발생 - bifId: {}", bifId, e);
            return createEmptyStatsResponse(bifId);
        }
    }

    private Map<EmotionType, Integer> calculateEmotionCounts(Long bifId, LocalDateTime yearMonth) {
        final LocalDateTime startOfMonth = yearMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        final LocalDateTime endOfMonth = yearMonth.withDayOfMonth(yearMonth.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        final List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfMonth, endOfMonth);

        if (monthlyDiaries.isEmpty()) {
            return initializeEmotionCounts();
        }

        final Map<EmotionType, Integer> emotionCounts = initializeEmotionCounts();

        for (Diary diary : monthlyDiaries) {
            final Emotion diaryEmotion = diary.getEmotion();
            final EmotionType statsEmotion = EmotionMapper.mapDiaryEmotionToStats(diaryEmotion);
            emotionCounts.put(statsEmotion, emotionCounts.get(statsEmotion) + 1);
        }

        return emotionCounts;
    }

    private List<String> extractKeywordsFromDiaries(Long bifId, LocalDateTime yearMonth) {
        try {
            final LocalDateTime startOfMonth = yearMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            final LocalDateTime endOfMonth = yearMonth.withDayOfMonth(yearMonth.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59);

            final List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfMonth, endOfMonth);
            
            if (monthlyDiaries.isEmpty()) {
                return List.of("일상", "생활", "하루");
            }

            final Set<String> keywords = new HashSet<>();
            for (Diary diary : monthlyDiaries) {
                if (diary.getContent() != null) {
                    final AiEmotionAnalysisService.EmotionAnalysisResult analysis = 
                            aiEmotionAnalysisService.analyzeEmotionFromText(diary.getContent());
                    keywords.addAll(analysis.getKeywords());
                }
            }

            return keywords.stream().limit(5).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("키워드 추출 중 오류 발생", e);
            return List.of("일상", "생활", "하루");
        }
    }

    private int calculateStreakCount(Long bifId) {
        try {
            final LocalDateTime now = LocalDateTime.now();
            final LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            
            final List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfMonth, now);
            
            if (monthlyDiaries.isEmpty()) return 0;
            
            return monthlyDiaries.size();
            
        } catch (Exception e) {
            log.error("연속 기록 수 계산 중 오류 발생", e);
            return 0;
        }
    }

    private String generateStatisticsText(Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        if (total == 0) {
            return "이번 달에는 작성된 일기가 없습니다. 첫 번째 일기를 작성해보세요!";
        }

        final EmotionType dominantEmotion = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(EmotionType.OKAY);

        final double dominantRatio = (double) emotionCounts.get(dominantEmotion) / total * 100;

        return String.format("이번 달에는 %s한 감정이 %.1f%%로 가장 많이 나타났습니다. %s", 
                dominantEmotion.getKoreanName(), dominantRatio, 
                dominantEmotion.isPositive() ? "긍정적인 감정을 많이 느끼셨네요!" : 
                dominantEmotion.isNegative() ? "힘든 일이 있었나요? 내일은 더 나아질 거예요." : 
                "감정이 비교적 안정적으로 유지되었습니다.");
    }

    private String generateGuardianAdvice(Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        if (total == 0) {
            return NO_ADVICE_MSG;
        }

        final double positiveRatio = (emotionCounts.get(EmotionType.GOOD) + emotionCounts.get(EmotionType.GREAT)) / (double) total * 100;
        final double negativeRatio = (emotionCounts.get(EmotionType.ANGRY) + emotionCounts.get(EmotionType.DOWN)) / (double) total * 100;

        if (positiveRatio > 60.0) {
            return "BIF가 매우 긍정적인 감정을 많이 느끼고 있습니다. 이런 좋은 기분을 유지할 수 있도록 지지해주세요.";
        } else if (negativeRatio > 40.0) {
            return "BIF가 부정적인 감정을 많이 경험하고 있습니다. 따뜻한 관심과 대화를 통해 도움을 주세요.";
        } else {
            return "BIF가 균형잡힌 감정 상태를 유지하고 있습니다. 이런 안정적인 상태를 지지해주세요.";
        }
    }

    private void saveEmotionCountsToStats(Long bifId, LocalDateTime yearMonth, Map<EmotionType, Integer> emotionCounts) {
        try {
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, yearMonth);
            final Stats stats = existingStats.orElseGet(() -> Stats.builder()
                    .bifId(bifId)
                    .yearMonth(yearMonth)
                    .emotionStatisticsText("")
                    .guardianAdviceText("")
                    .build());

            final String emotionCountsJson = objectMapper.writeValueAsString(emotionCounts);
            stats.setEmotionCounts(emotionCountsJson);
            statsRepository.save(stats);

        } catch (Exception e) {
            log.error("감정 카운트 저장 중 오류 발생", e);
        }
    }

    private StatsResponse createEmptyStatsResponse(Long bifId) {
        try {
            final ProfileMeta meta = loadProfileMeta(bifId);
            return buildEmptyStatsResponse(bifId, meta.nickname, meta.joinDate, meta.totalDiaryCount, meta.connectionCode);
        } catch (Exception e) {
            log.error("빈 통계 응답 생성 실패 - bifId: {}", bifId, e);
            return buildEmptyStatsResponse(bifId, "BIF", LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT)), 0, "");
        }
    }

    private StatsResponse buildEmptyStatsResponse(Long bifId, String nickname, String joinDate, 
                                                int totalDiaryCount, String connectionCode) {
        return StatsResponse.builder()
                .statisticsText("통계 데이터를 생성 중입니다. 잠시 후 다시 조회해주세요.")
                .guardianAdviceText(NO_ADVICE_MSG)
                .emotionRatio(Collections.emptyList())
                .topKeywords(Collections.emptyList())
                .monthlyChange(Collections.emptyList())
                .bifId(bifId)
                .nickname(nickname)
                .joinDate(joinDate)
                .totalDiaryCount(totalDiaryCount)
                .connectionCode(connectionCode)
                .characterInfo(createDefaultCharacterInfo())
                .achievementInfo(createDefaultAchievementInfo())
                .emotionTrends(Collections.emptyList())
                .build();
    }

    private StatsResponse.CharacterInfo createDefaultCharacterInfo() {
        return StatsResponse.CharacterInfo.builder()
                .name("현명한 거북이")
                .message("오늘 하루도 수고하셨어요. 내일은 더 좋은 하루가 될 거예요! 🐢")
                .emoji("🐢")
                .mood("평온")
                .advice("첫 번째 일기를 작성해보세요!")
                .build();
    }

    private StatsResponse.AchievementInfo createDefaultAchievementInfo() {
        return StatsResponse.AchievementInfo.builder()
                .totalPoints(0)
                .currentLevel(1)
                .levelTitle("감정 탐험가")
                .recentAchievements(Collections.emptyList())
                .streakCount(0)
                .nextMilestone("10점 달성하여 첫 업적 획득")
                .build();
    }

    private record ProfileMeta(String nickname, String joinDate, String connectionCode, int totalDiaryCount) {}

    private ProfileMeta loadProfileMeta(Long bifId) {
        final var bifOpt = bifRepository.findById(bifId);
        final String nickname = bifOpt.map(Bif::getNickname).orElse("BIF");
        final String joinDate = bifOpt.map(bif -> bif.getCreatedAt().format(DateTimeFormatter.ofPattern(DATE_FORMAT))).orElse("");
        final String connectionCode = bifOpt.map(Bif::getConnectionCode).orElse("");
        final int totalDiaryCount = calculateTotalDiaryCount(bifId);
        return new ProfileMeta(nickname, joinDate, connectionCode, totalDiaryCount);
    }

    private int calculateTotalDiaryCount(Long bifId) {
        try {
            final LocalDateTime startOfYear = LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            final LocalDateTime endOfYear = LocalDateTime.now().withMonth(12).withDayOfMonth(31).withHour(23).withMinute(59).withSecond(59);
            
            final List<Diary> yearlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfYear, endOfYear);
            return yearlyDiaries.size();
        } catch (Exception e) {
            log.error("총 일기 수 계산 실패 - bifId: {}", bifId, e);
            return 0;
        }
    }

    private Map<EmotionType, Integer> initializeEmotionCounts() {
        final Map<EmotionType, Integer> counts = new EnumMap<>(EmotionType.class);
        for (EmotionType emotion : EmotionType.values()) {
            counts.put(emotion, 0);
        }
        return counts;
    }

    private String getBifNickname(Long bifId) {
        try {
            final Bif bif = bifRepository.findById(bifId)
                    .orElseThrow(() -> new RuntimeException("BIF not found: " + bifId));
            return bif.getNickname();
        } catch (Exception e) {
            log.error("BIF 닉네임 조회 실패 - bifId: {}", bifId, e);
            return "BIF_" + bifId;
        }
    }

    private String getGuardianJoinDateByBifId(Long bifId) {
        try {
            final List<LocalDateTime> results = entityManager.createQuery(
                    "select g.createdAt from com.sage.bif.user.entity.Guardian g where g.bif.bifId = :bifId order by g.createdAt asc",
                    LocalDateTime.class)
                    .setParameter("bifId", bifId)
                    .setMaxResults(1)
                    .getResultList();

            if (results.isEmpty()) {
                return "";
            }

            return results.get(0).format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch (Exception e) {
            log.warn("가디언 가입일 조회 실패 - bifId: {}", bifId, e);
            return "";
        }
    }

    private LocalDateTime getCurrentYearMonth() {
        final LocalDateTime now = LocalDateTime.now();
        return LocalDateTime.of(now.getYear(), now.getMonth(), 1, 0, 0, 0);
    }

    private StatsResponse buildStatsResponseWithRealTimeData(Stats statsData, Long bifId, LocalDateTime yearMonth) {
        try {
            final Map<EmotionType, Integer> emotionCounts = parseEmotionCountsJson(statsData.getEmotionCounts());
            final List<StatsResponse.EmotionRatio> emotionRatio = calculateEmotionRatio(emotionCounts);
            final List<StatsResponse.KeywordData> topKeywords = createKeywordDataList(parseTopKeywordsJson(statsData.getTopKeywords()));
            final List<StatsResponse.MonthlyChange> monthlyChange = getMonthlyChange(bifId, yearMonth);
            final ProfileMeta meta = loadProfileMeta(bifId);

            final StatsResponse.CharacterInfo characterInfo = createCharacterInfo(statsData);
            final StatsResponse.AchievementInfo achievementInfo = createAchievementInfo(bifId, emotionCounts, topKeywords);
            final List<StatsResponse.EmotionTrend> emotionTrends = createEmotionTrends(bifId, yearMonth);

            return StatsResponse.builder()
                    .statisticsText(statsData.getEmotionStatisticsText())
                    .guardianAdviceText(statsData.getGuardianAdviceText())
                    .emotionRatio(emotionRatio)
                    .topKeywords(topKeywords)
                    .monthlyChange(monthlyChange)
                    .bifId(bifId)
                    .nickname(meta.nickname)
                    .joinDate(meta.joinDate)
                    .totalDiaryCount(meta.totalDiaryCount)
                    .connectionCode(meta.connectionCode)
                    .characterInfo(characterInfo)
                    .achievementInfo(achievementInfo)
                    .emotionTrends(emotionTrends)
                    .build();

        } catch (Exception e) {
            log.error("통계 응답 생성 중 오류 발생", e);
            throw new RuntimeException("통계 응답 생성 실패", e);
        }
    }

    private StatsResponse.CharacterInfo createCharacterInfo(Stats statsData) {
        return StatsResponse.CharacterInfo.builder()
                .name(statsData.getCharacterName() != null ? statsData.getCharacterName() : "현명한 거북이")
                .message(statsData.getCharacterMessage() != null ? statsData.getCharacterMessage() : "오늘 하루도 수고하셨어요! 🐢")
                .emoji(getCharacterEmoji(statsData.getCharacterName()))
                .mood(determineMood(statsData.getAiEmotionScore()))
                .advice(generateCharacterAdvice(statsData.getAiEmotionScore()))
                .build();
    }

    private String getCharacterEmoji(String characterName) {
        if (characterName == null) return "🐢";
        return switch (characterName) {
            case "행복한 토끼" -> "🐰";
            case "즐거운 강아지" -> "🐕";
            case "현명한 거북이" -> "🐢";
            case "걱정 많은 고양이" -> "🐱";
            case "화난 호랑이" -> "🐯";
            default -> "🐢";
        };
    }

    private String determineMood(Double aiEmotionScore) {
        if (aiEmotionScore == null) return "평온";
        if (aiEmotionScore >= 1.5) return "매우 행복";
        if (aiEmotionScore >= 0.5) return "기쁨";
        if (aiEmotionScore >= -0.5) return "평온";
        if (aiEmotionScore >= -1.5) return "우울";
        return "화남";
    }

    private String generateCharacterAdvice(Double aiEmotionScore) {
        if (aiEmotionScore == null) return "오늘 하루도 수고하셨어요!";
        if (aiEmotionScore >= 1.5) return "이런 좋은 기분을 계속 유지해보세요! 🎉";
        if (aiEmotionScore >= 0.5) return "조금 더 긍정적인 마음가짐으로 하루를 보내보세요! 😊";
        if (aiEmotionScore >= -0.5) return "내일은 더 좋은 하루가 될 거예요! 🐢";
        if (aiEmotionScore >= -1.5) return "내일은 더 나아질 거예요! 🐱";
        return "힘내세요! 내일은 분명 좋아질 거예요! 🐯";
    }

    private StatsResponse.AchievementInfo createAchievementInfo(Long bifId, Map<EmotionType, Integer> emotionCounts, 
                                                               List<StatsResponse.KeywordData> topKeywords) {
        try {
            final int diaryCount = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
            final int streakCount = calculateStreakCount(bifId);
            final List<String> emotions = emotionCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(entry -> entry.getKey().name())
                    .collect(Collectors.toList());
            final List<String> keywords = topKeywords.stream()
                    .map(StatsResponse.KeywordData::getKeyword)
                    .collect(Collectors.toList());

            final AchievementService.AchievementResult achievementResult = 
                    achievementService.calculateAchievements(bifId, diaryCount, streakCount, emotions, keywords);

            return StatsResponse.AchievementInfo.builder()
                    .totalPoints(achievementResult.getTotalPoints())
                    .currentLevel(achievementResult.getCurrentLevel())
                    .levelTitle(achievementResult.getLevelTitle())
                    .recentAchievements(convertToResponseAchievements(achievementResult.getRecentAchievements()))
                    .streakCount(achievementResult.getStreakCount())
                    .nextMilestone(achievementResult.getNextMilestone())
                    .build();

        } catch (Exception e) {
            log.error("업적 정보 생성 중 오류 발생", e);
            return createDefaultAchievementInfo();
        }
    }

    private List<StatsResponse.Achievement> convertToResponseAchievements(List<AchievementService.Achievement> achievements) {
        return achievements.stream()
                .map(achievement -> StatsResponse.Achievement.builder()
                        .name(achievement.getName())
                        .description(achievement.getDescription())
                        .points(achievement.getPoints())
                        .icon(achievement.getIcon())
                        .earnedAt(achievement.getEarnedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<StatsResponse.EmotionTrend> createEmotionTrends(Long bifId, LocalDateTime yearMonth) {
        try {
            final List<StatsResponse.EmotionTrend> trends = new ArrayList<>();
            final LocalDateTime startOfMonth = yearMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            final LocalDateTime endOfMonth = yearMonth.withDayOfMonth(yearMonth.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59);

            final List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfMonth, endOfMonth);

            if (monthlyDiaries.isEmpty()) {
                return trends;
            }

            for (int day = 1; day <= yearMonth.toLocalDate().lengthOfMonth(); day++) {
                final LocalDateTime dayStart = yearMonth.withDayOfMonth(day).withHour(0).withMinute(0).withSecond(0);
                final LocalDateTime dayEnd = yearMonth.withDayOfMonth(day).withHour(23).withMinute(59).withSecond(59);

                final List<Diary> dayDiaries = monthlyDiaries.stream()
                        .filter(diary -> !diary.getCreatedAt().isBefore(dayStart) && !diary.getCreatedAt().isAfter(dayEnd))
                        .collect(Collectors.toList());

                if (!dayDiaries.isEmpty()) {
                    final double averageScore = dayDiaries.stream()
                            .mapToDouble(diary -> {
                                final EmotionType emotionType = EmotionMapper.mapDiaryEmotionToStats(diary.getEmotion());
                                return emotionType.getScore();
                            })
                            .average()
                            .orElse(0.0);

                    final EmotionType dominantEmotion = EmotionType.fromScore(averageScore);
                    final String trend = determineTrend(averageScore);
                    final String description = generateTrendDescription(dominantEmotion, averageScore);

                    trends.add(StatsResponse.EmotionTrend.builder()
                            .date(dayStart.format(DateTimeFormatter.ofPattern("MM-dd")))
                            .dominantEmotion(dominantEmotion)
                            .averageScore(averageScore)
                            .trend(trend)
                            .description(description)
                            .build());
                }
            }

            return trends;

        } catch (Exception e) {
            log.error("감정 트렌드 생성 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    private String determineTrend(double averageScore) {
        if (averageScore >= 1.0) return "상승";
        if (averageScore >= 0.0) return "안정";
        return "하락";
    }

    private String generateTrendDescription(EmotionType dominantEmotion, double averageScore) {
        if (averageScore >= 1.5) return "매우 긍정적인 하루";
        if (averageScore >= 0.5) return "긍정적인 하루";
        if (averageScore >= -0.5) return "평온한 하루";
        if (averageScore >= -1.5) return "조금 힘든 하루";
        return "힘든 하루";
    }

    private List<StatsResponse.EmotionRatio> calculateEmotionRatio(Map<EmotionType, Integer> emotionCounts) {
        final int totalCount = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (totalCount == 0) {
            return Collections.emptyList();
        }

        final List<StatsResponse.EmotionRatio> emotionRatios = new ArrayList<>();

        for (final Map.Entry<EmotionType, Integer> entry : emotionCounts.entrySet()) {
            final EmotionType emotionType = entry.getKey();
            final Integer count = entry.getValue();
            final double percentage = (double) count / totalCount * 100;

            final StatsResponse.EmotionRatio ratio = StatsResponse.EmotionRatio.builder()
                    .emotion(emotionType)
                    .value(count)
                    .emoji(emotionType.getEmoji())
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .build();

            emotionRatios.add(ratio);
        }

        emotionRatios.sort((a, b) -> {
            final int orderA = getEmotionOrder(a.getEmotion());
            final int orderB = getEmotionOrder(b.getEmotion());
            return Integer.compare(orderA, orderB);
        });

        return emotionRatios;
    }

    private int getEmotionOrder(EmotionType emotionType) {
        return switch (emotionType) {
            case GREAT -> 1;
            case GOOD -> 2;
            case OKAY -> 3;
            case DOWN -> 4;
            case ANGRY -> 5;
        };
    }

    private Map<EmotionType, Integer> parseEmotionCountsJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new EnumMap<>(EmotionType.class);
        }

        try {
            final String cleanedJson = unwrapIfQuoted(json.trim());
            final Map<String, Integer> rawCounts = objectMapper.readValue(cleanedJson, new TypeReference<>() {});
            return processEmotionCountsFromRaw(rawCounts);
        } catch (Exception e) {
            log.error("감정 카운트 JSON 파싱 실패: {}", json, e);
            return new EnumMap<>(EmotionType.class);
        }
    }

    private Map<EmotionType, Integer> processEmotionCountsFromRaw(Map<String, Integer> rawCounts) {
        final Map<EmotionType, Integer> emotionCounts = new EnumMap<>(EmotionType.class);
        for (EmotionType emotion : EmotionType.values()) {
            emotionCounts.put(emotion, 0);
        }
        
        for (Map.Entry<String, Integer> entry : rawCounts.entrySet()) {
            try {
                final EmotionType emotionType = mapDbeaverEmotionToStatsEmotion(entry.getKey());
                emotionCounts.put(emotionType, entry.getValue());
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 감정 타입: {}", entry.getKey());
            }
        }
        
        return emotionCounts;
    }

    private EmotionType mapDbeaverEmotionToStatsEmotion(String dbeaverEmotion) {
        return switch (dbeaverEmotion.toUpperCase()) {
            case "JOY" -> EmotionType.GOOD;
            case "SAD" -> EmotionType.DOWN;
            case "ANGER" -> EmotionType.ANGRY;
            case "NEUTRAL" -> EmotionType.OKAY;
            case "EXCELLENT" -> EmotionType.GREAT;
            case "ANGRY" -> EmotionType.ANGRY;
            case "DOWN" -> EmotionType.DOWN;
            case "GOOD" -> EmotionType.GOOD;
            case "GREAT" -> EmotionType.GREAT;
            case "OKAY" -> EmotionType.OKAY;
            default -> {
                log.warn("알 수 없는 감정 타입: {}, 기본값 OKAY 사용", dbeaverEmotion);
                yield EmotionType.OKAY;
            }
        };
    }

    private List<Map<String, Object>> parseTopKeywordsJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            final String cleanedJson = unwrapIfQuoted(json.trim());
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("키워드 JSON 파싱 실패: {}", json, e);
            return createDefaultTopKeywords();
        }
    }

    private List<Map<String, Object>> createDefaultTopKeywords() {
        final List<Map<String, Object>> defaultKeywords = new ArrayList<>();
        final String[] defaultWords = {"가족", "직장", "친구", "휴식", "건강"};

        for (int i = 0; i < defaultWords.length; i++) {
            final Map<String, Object> keywordData = new HashMap<>();
            keywordData.put("keyword", defaultWords[i]);
            keywordData.put("count", 0);
            keywordData.put("rank", i + 1);
            defaultKeywords.add(keywordData);
        }

        return defaultKeywords;
    }

    private List<StatsResponse.KeywordData> createKeywordDataList(List<Map<String, Object>> topKeywordsData) {
        return topKeywordsData.stream()
                .map(keyword -> StatsResponse.KeywordData.builder()
                        .keyword((String) keyword.get("keyword"))
                        .count((Integer) keyword.get("count"))
                        .rank((Integer) keyword.get("rank"))
                        .build())
                .collect(Collectors.toList());
    }

    private List<StatsResponse.MonthlyChange> getMonthlyChange(Long bifId, LocalDateTime yearMonth) {
        final LocalDateTime lastYearMonth = yearMonth.minusMonths(1);

        final Optional<Stats> currentStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, yearMonth);
        final Optional<Stats> lastStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, lastYearMonth);

        final List<StatsResponse.MonthlyChange> monthlyChange = new ArrayList<>();

        try {
            final Map<EmotionType, Integer> currentCounts = parseEmotionCountsFromStats(currentStats);
            final Map<EmotionType, Integer> lastCounts = parseEmotionCountsFromStats(lastStats);

            for (EmotionType emotion : EmotionType.values()) {
                final Integer currentValue = currentCounts.getOrDefault(emotion, 0);
                final Integer previousValue = lastCounts.getOrDefault(emotion, 0);

                monthlyChange.add(buildMonthlyComparisonItem(emotion, currentValue, previousValue));
            }

            monthlyChange.sort((a, b) -> {
                final int orderA = getEmotionOrder(a.getEmotion());
                final int orderB = getEmotionOrder(b.getEmotion());
                return Integer.compare(orderA, orderB);
            });

        } catch (Exception e) {
            log.error("월별 감정 비교 데이터 생성 중 오류 발생", e);
            addDefaultMonthlyChangeItems(monthlyChange);
        }

        return monthlyChange;
    }

    private Map<EmotionType, Integer> parseEmotionCountsFromStats(Optional<Stats> statsOpt) {
        if (statsOpt.isPresent() && statsOpt.get().getEmotionCounts() != null && !statsOpt.get().getEmotionCounts().trim().isEmpty()) {
            try {
                final String countsJson = unwrapIfQuoted(statsOpt.get().getEmotionCounts());
                final Map<String, Integer> rawCounts = objectMapper.readValue(countsJson, new TypeReference<>() {});
                return processEmotionCountsFromRaw(rawCounts);
            } catch (Exception e) {
                log.error("통계에서 감정 카운트 파싱 실패", e);
                return new EnumMap<>(EmotionType.class);
            }
        }
        return new EnumMap<>(EmotionType.class);
    }

    private StatsResponse.MonthlyChange buildMonthlyComparisonItem(EmotionType emotion, Integer currentValue, Integer previousValue) {
        final Double changePercentage = calculateChangePercentage(previousValue, currentValue);
        final String changeStatus = determineChangeStatus(currentValue, previousValue);
        final String changeDescription = generateChangeDescription(emotion, currentValue, previousValue, changePercentage);

        return StatsResponse.MonthlyChange.builder()
                .month("감정별 변화")
                .emotion(emotion)
                .value(currentValue)
                .previousValue(previousValue)
                .changePercentage(changePercentage)
                .changeStatus(changeStatus)
                .changeDescription(changeDescription)
                .build();
    }

    private String determineChangeStatus(Integer currentValue, Integer previousValue) {
        if (previousValue == 0) {
            return currentValue > 0 ? "NEW" : "SAME";
        }

        final double change = ((double) (currentValue - previousValue) / previousValue) * 100.0;

        if (change > 20.0) {
            return "INCREASE";
        } else if (change < -20.0) {
            return "DECREASE";
        } else {
            return "STABLE";
        }
    }

    private String generateChangeDescription(EmotionType emotion, Integer currentValue, Integer previousValue, Double changePercentage) {
        if (previousValue == 0) {
            if (currentValue > 0) {
                return String.format("지난달에는 없었던 %s 감정이 이번달에 %d회 나타났습니다.", emotion.getKoreanName(), currentValue);
            }
            return String.format("지난달과 이번달 모두 %s 감정이 나타나지 않았습니다.", emotion.getKoreanName());
        }

        if (changePercentage > 20.0) {
            return String.format("지난달보다 %s 감정이 %.1f%% 증가했습니다.", emotion.getKoreanName(), changePercentage);
        } else if (changePercentage < -20.0) {
            return String.format("지난달보다 %s 감정이 %.1f%% 감소했습니다.", emotion.getKoreanName(), Math.abs(changePercentage));
        } else {
            return String.format("지난달과 비슷한 수준의 %s 감정을 보였습니다.", emotion.getKoreanName());
        }
    }

    private void addDefaultMonthlyChangeItems(List<StatsResponse.MonthlyChange> monthlyChange) {
        for (EmotionType emotion : EmotionType.values()) {
            monthlyChange.add(StatsResponse.MonthlyChange.builder()
                    .month("감정별 변화")
                    .emotion(emotion)
                    .value(0)
                    .previousValue(0)
                    .changePercentage(0.0)
                    .build());
        }
    }

    private Double calculateChangePercentage(Integer previousValue, Integer currentValue) {
        if (previousValue == 0) {
            return currentValue > 0 ? 100.0 : 0.0;
        }
        return ((double) (currentValue - previousValue) / previousValue) * 100.0;
    }

    private String unwrapIfQuoted(String jsonLike) {
        if (jsonLike == null || jsonLike.isEmpty()) return jsonLike;
        final String trimmed = jsonLike.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            String result = trimmed.substring(1, trimmed.length() - 1);
            result = result.replace("\\\"", "\"");
            return result;
        }
        return trimmed;
    }
}
