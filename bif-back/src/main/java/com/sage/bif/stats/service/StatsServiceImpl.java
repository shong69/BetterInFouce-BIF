package com.sage.bif.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.stats.dto.GuardianStatsResponse;
import com.sage.bif.stats.dto.StatsResponse;
import com.sage.bif.stats.entity.EmotionType;
import com.sage.bif.stats.entity.Stats;
import com.sage.bif.stats.exception.StatsProcessingException;
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
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StatsServiceImpl implements StatsService, ApplicationContextAware {



    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String NO_ADVICE_MSG = "BIF의 감정 데이터가 없어 조언을 제공할 수 없습니다.";
    private static final int MAX_KEYWORDS = 5;
    private static final int MAX_KEYWORD_DISPLAY_LENGTH = 100;
    private static final double CHANGE_THRESHOLD = 20.0;
    private static final int DEFAULT_LEVEL = 1;
    private static final int DEFAULT_POINTS = 0;
    private static final int HOUR_23 = 23;
    private static final int MINUTE_59 = 59;
    private static final int SECOND_59 = 59;

    private final StatsRepository statsRepository;
    private final DiaryRepository diaryRepository;
    private final BifRepository bifRepository;
    private final AiEmotionAnalysisService aiEmotionAnalysisService;
    private final AchievementService achievementService;
    private final KeywordAccumulationService keywordAccumulationService;
    private final ObjectMapper objectMapper;
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(@org.springframework.lang.NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    private static final String KEYWORD_KEY = "keyword";
    private static final String NORMALIZED_VALUE_KEY = "normalizedValue";
    private static final String COUNT_KEY = "count";
    private static final String RANK_KEY = "rank";
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public StatsResponse getMonthlyStats(final Long bifId) {
        try {
            log.info("BIF ID {}의 월별 통계 조회 시작", bifId);
            
        final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);

            if (existingStats.isEmpty()) {
                log.info("BIF ID {}의 통계 데이터가 없어 새로 생성합니다.", bifId);
                return generateAndSaveMonthlyStats(bifId, currentYearMonth);
            }

            final Map<EmotionType, Integer> realTimeEmotionCounts = calculateEmotionCounts(bifId, currentYearMonth);
            final Stats stats = existingStats.get();
            
            final Map<EmotionType, Integer> storedEmotionCounts = parseEmotionCountsJson(stats.getEmotionCounts());
            if (!realTimeEmotionCounts.equals(storedEmotionCounts)) {
                log.info("BIF ID {}의 감정 카운트 변경 감지 - 실시간 업데이트", bifId);
                stats.setEmotionCounts(objectMapper.writeValueAsString(realTimeEmotionCounts));
                stats.setEmotionStatisticsText(generateStatisticsText(realTimeEmotionCounts));
                stats.setGuardianAdviceText(generateGuardianAdvice(realTimeEmotionCounts));
                statsRepository.save(stats);
            }

            return buildStatsResponseWithRealTimeData(stats, bifId, currentYearMonth);
            
        } catch (Exception e) {
            log.error("월별 통계 조회 중 오류 발생 - bifId: {}", bifId, e);
            return createEmptyStatsResponse(bifId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianStatsResponse getGuardianStats(final Long bifId) {
        log.info("보호자가 BIF ID {}의 통계를 조회합니다.", bifId);

        try {

            final StatsResponse bifStats = applicationContext.getBean(StatsServiceImpl.class).getMonthlyStats(bifId);
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
            throw new StatsProcessingException("보호자 통계 조회 실패", e);
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
                
                final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, currentYearMonth);
                stats.setEmotionCounts(objectMapper.writeValueAsString(emotionCounts));
                
                final AiEmotionAnalysisService.EmotionAnalysisResult analysis = aiEmotionAnalysisService.analyzeEmotionFromText(diaryContent);
                stats.setAiEmotionScore(analysis.getEmotionScore());
                
                keywordAccumulationService.updateKeywordsWithNewContent(bifId, analysis.getKeywords(), diaryContent);
                
                stats.setEmotionStatisticsText(generateStatisticsText(emotionCounts));
                stats.setGuardianAdviceText(generateGuardianAdvice(emotionCounts));
                
                statsRepository.save(stats);

                log.info("BIF ID {}의 실시간 통계 업데이트 완료 - 감정 카운트: {}", bifId, emotionCounts);
            } else {
                log.info("BIF ID {}의 통계 데이터가 없어 새로 생성합니다.", bifId);
                generateAndSaveMonthlyStats(bifId, currentYearMonth);
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
            applicationContext.getBean(StatsServiceImpl.class).generateMonthlyStats(bifId, yearMonth);
        } catch (Exception e) {
            log.error("비동기 통계 생성 중 오류 발생 - bifId: {}, yearMonth: {}", bifId, yearMonth, e);
        }
    }


    
    private StatsResponse generateAndSaveMonthlyStats(Long bifId, LocalDateTime yearMonth) {
        try {
            final MonthlyDiaryData monthlyData = loadMonthlyDiaryData(bifId, yearMonth);
            
            final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(monthlyData);

            final Map<String, Integer> keywordFrequency = buildKeywordFrequencyMap(monthlyData);

            final AiEmotionAnalysisService.EmotionAnalysisResult aiAnalysis = 
                    aiEmotionAnalysisService.analyzeEmotionFromText("");

            final Stats stats = Stats.builder()
                    .bifId(bifId)
                    .yearMonth(yearMonth)
                    .emotionStatisticsText(generateStatisticsText(emotionCounts))
                    .guardianAdviceText(generateGuardianAdvice(emotionCounts))
                    .emotionCounts(objectMapper.writeValueAsString(emotionCounts))
                    .topKeywords(objectMapper.writeValueAsString(keywordFrequency))
                    .aiEmotionScore(aiAnalysis.getEmotionScore())
                    .build();

            final Stats savedStats = statsRepository.save(stats);
            log.info("BIF ID {}의 통계 데이터 저장 완료", bifId);

            return buildStatsResponseWithRealTimeData(savedStats, monthlyData);
            
        } catch (Exception e) {
            log.error("통계 데이터 생성 및 저장 중 오류 발생 - bifId: {}", bifId, e);
            return createEmptyStatsResponse(bifId);
        }
    }

    private Map<EmotionType, Integer> calculateEmotionCounts(Long bifId, LocalDateTime yearMonth) {
        final LocalDateTime startOfMonth = yearMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        final LocalDateTime endOfMonth = yearMonth.withDayOfMonth(yearMonth.toLocalDate().lengthOfMonth())
                .withHour(HOUR_23).withMinute(MINUTE_59).withSecond(SECOND_59);

        final List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfMonth, endOfMonth);
        return calculateEmotionCountsFromDiaries(monthlyDiaries);
    }

    private Map<EmotionType, Integer> calculateEmotionCounts(MonthlyDiaryData monthlyData) {
        return calculateEmotionCountsFromDiaries(monthlyData.getDiaries());
    }

    private Map<EmotionType, Integer> calculateEmotionCountsFromDiaries(List<Diary> monthlyDiaries) {
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



    private List<String> validateKeywords(String content, List<String> aiKeywords) {
        final List<String> validatedKeywords = new ArrayList<>();
        final String lowerContent = content.toLowerCase();
        
        for (String keyword : aiKeywords) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                final String trimmedKeyword = keyword.trim();
                // 키워드가 실제 일기 내용에 포함되어 있는지 확인
                if (lowerContent.contains(trimmedKeyword.toLowerCase())) {
                    validatedKeywords.add(trimmedKeyword);
                } else {
                    log.debug("키워드 '{}'가 일기 내용에 포함되지 않음", trimmedKeyword);
                }
            }
        }
        
        return validatedKeywords;
    }



    private void extractKeywordsFromContent(String content, Set<String> fallbackKeywords) {
        final String[] meaningfulKeywords = {
            "회의", "미팅", "프로젝트", "업무", "일", "직장", "회사",
            "가족", "친구", "동료", "사람",
            "학교", "대학교", "수업", "공부", "시험", "과제",
            "운동", "헬스", "등산", "조깅", "수영",
            "음식", "요리", "맛집", "카페", "레스토랑",
            "여행", "휴가", "출장", "여행지", "관광",
            "영화", "드라마", "음악", "책", "독서", "게임",
            "취미", "관심사", "새로운", "도전"
        };
        
        for (String keyword : meaningfulKeywords) {
            if (content.contains(keyword)) {
                fallbackKeywords.add(keyword);
                break;
            }
        }
        
        if (content.contains("역") || content.contains("역사")) {
            fallbackKeywords.add("교통");
        }
        if (content.contains("집") || content.contains("집에")) {
            fallbackKeywords.add("집");
        }
        if (content.contains("회사") || content.contains("직장")) {
            fallbackKeywords.add("직장");
        }
    }

    private List<String> extractFallbackKeywords(List<Diary> diaries) {
        final Set<String> fallbackKeywords = new HashSet<>();
        
        for (Diary diary : diaries) {
            if (diary.getContent() != null && !diary.getContent().trim().isEmpty()) {
                final String content = diary.getContent().trim();
                extractKeywordsFromContent(content, fallbackKeywords);
            }
        }
        
        final List<String> result = fallbackKeywords.stream().limit(5).toList();
        log.info("의미 있는 Fallback 키워드 추출: {}", result);
        return result;
    }

    private Map<String, Integer> analyzeMonthlyDiariesForKeywords(MonthlyDiaryData monthlyData) {
        return analyzeMonthlyDiariesForKeywordsFromList(monthlyData.getDiaries());
    }

    private Map<String, Integer> analyzeMonthlyDiariesForKeywordsFromList(List<Diary> monthlyDiaries) {
        if (monthlyDiaries.isEmpty()) {
            log.info("월간 일기가 없음 - 빈 맵 반환");
            return new HashMap<>();
        }

        final Map<String, Integer> keywordFrequency = new HashMap<>();
        for (Diary diary : monthlyDiaries) {
            if (diary.getContent() != null && !diary.getContent().trim().isEmpty()) {
                processDiaryKeywords(diary, keywordFrequency);
            } else {
                log.warn("일기 ID {}의 내용이 비어있음", diary.getId());
            }
        }
        
        log.info("최종 키워드 빈도수: {}", keywordFrequency);
        
        final Map<String, Integer> top5Keywords = keywordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_KEYWORDS)
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
        
        log.info("정규화된 TOP 5 키워드: {}", top5Keywords);
        
        return top5Keywords;
    }

    private Map<String, Integer> buildKeywordFrequencyMap(MonthlyDiaryData monthlyData) {
        try {
            log.info("=== 키워드 빈도수 맵 생성 시작 - BIF ID: {}, 월: {} ===", 
                monthlyData.getBifId(), monthlyData.getYearMonth().getMonthValue());

            final Map<String, Integer> accumulatedKeywords = keywordAccumulationService
                .getKeywordFrequency(monthlyData.getBifId(), monthlyData.getYearMonth());
            if (!accumulatedKeywords.isEmpty()) {
                log.info("누적된 키워드 사용: {}", accumulatedKeywords);
                return accumulatedKeywords;
            }

            final Map<String, Integer> newKeywords = analyzeMonthlyDiariesForKeywords(monthlyData);
            
            if (!newKeywords.isEmpty()) {
                keywordAccumulationService.initializeKeywords(monthlyData.getBifId(), newKeywords);
            }
            
            return newKeywords;

        } catch (Exception e) {
            log.error("키워드 빈도수 맵 생성 중 오류 발생 - bifId: {}", monthlyData.getBifId(), e);
            return new HashMap<>();
        }
    }

    private List<String> extractAiKeywords(Diary diary, String content) {
        List<String> extractedKeywords = new ArrayList<>();
        try {
            final AiEmotionAnalysisService.EmotionAnalysisResult analysis = 
                    aiEmotionAnalysisService.analyzeEmotionFromText(content);
            
            if (analysis.getKeywords() != null && !analysis.getKeywords().isEmpty()) {
                final List<String> validatedKeywords = validateKeywords(content, analysis.getKeywords());
                extractedKeywords.addAll(validatedKeywords);
                log.info("일기 ID {}에서 AI 키워드 추출 성공: {}", diary.getId(), validatedKeywords);
            } else {
                log.warn("일기 ID {}에서 AI 키워드 추출 실패 - 빈 결과", diary.getId());
            }
        } catch (Exception e) {
            log.warn("일기 ID {}에서 AI 키워드 추출 중 오류: {}", diary.getId(), e.getMessage());
        }
        return extractedKeywords;
    }

    private void processDiaryKeywords(Diary diary, Map<String, Integer> keywordFrequency) {
        try {
            final String content = diary.getContent().trim();
            log.info("일기 ID {} 분석 - 내용: {}", diary.getId(), content.substring(0, Math.min(MAX_KEYWORD_DISPLAY_LENGTH, content.length())));
            
            List<String> extractedKeywords = extractAiKeywords(diary, content);
            
            if (extractedKeywords.isEmpty()) {
                log.info("일기 ID {}에서 fallback 키워드 사용", diary.getId());
                final List<String> fallbackKeywords = extractFallbackKeywords(List.of(diary));
                extractedKeywords.addAll(fallbackKeywords);
            }
            
            final Set<String> diaryKeywords = new HashSet<>();
            for (String keyword : extractedKeywords) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    final String normalizedKeyword = keyword.trim();
                    if (!diaryKeywords.contains(normalizedKeyword)) {
                        diaryKeywords.add(normalizedKeyword);
                        keywordFrequency.put(normalizedKeyword, keywordFrequency.getOrDefault(normalizedKeyword, 0) + 1);
                        log.info("키워드 '{}' 누적: {}회 (일기 ID: {})", normalizedKeyword, keywordFrequency.get(normalizedKeyword), diary.getId());
                    } else {
                        log.info("일기 ID {}에서 키워드 '{}' 중복 제거", diary.getId(), normalizedKeyword);
                    }
                }
            }

        } catch (Exception e) {
            log.error("일기 ID {}의 키워드 분석 실패: {}", diary.getId(), e.getMessage());
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
        return aiEmotionAnalysisService.generateStatisticsTextWithAI(emotionCounts);
    }

    private String generateGuardianAdvice(Map<EmotionType, Integer> emotionCounts) {
        return aiEmotionAnalysisService.generateGuardianAdviceWithAI(emotionCounts);
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
                .totalPoints(DEFAULT_POINTS)
                .currentLevel(DEFAULT_LEVEL)
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

            final StatsResponse.CharacterInfo characterInfo = createCharacterInfo();
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
            throw new StatsProcessingException("통계 응답 생성 실패", e);
        }
    }

    private StatsResponse buildStatsResponseWithRealTimeData(Stats statsData, MonthlyDiaryData monthlyData) {
        try {
            final Long bifId = monthlyData.getBifId();
            final LocalDateTime yearMonth = monthlyData.getYearMonth();
            
            final Map<EmotionType, Integer> emotionCounts = parseEmotionCountsJson(statsData.getEmotionCounts());
            final List<StatsResponse.EmotionRatio> emotionRatio = calculateEmotionRatio(emotionCounts);
            final List<StatsResponse.KeywordData> topKeywords = createKeywordDataList(parseTopKeywordsJson(statsData.getTopKeywords()));
            final List<StatsResponse.MonthlyChange> monthlyChange = getMonthlyChange(bifId, yearMonth);
            final ProfileMeta meta = loadProfileMeta(bifId);

            final StatsResponse.CharacterInfo characterInfo = createCharacterInfo();
            final StatsResponse.AchievementInfo achievementInfo = createAchievementInfo(bifId, emotionCounts, topKeywords);
            final List<StatsResponse.EmotionTrend> emotionTrends = createEmotionTrends(monthlyData);

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
            throw new StatsProcessingException("통계 응답 생성 실패", e);
        }
    }

    private StatsResponse.CharacterInfo createCharacterInfo() {
        return StatsResponse.CharacterInfo.builder()
                .name("현명한 거북이")
                .message("오늘 하루도 수고하셨어요! 🐢")
                .emoji("🐢")
                .mood("평온")
                .advice("내일은 더 좋은 하루가 될 거예요!")
                .build();
    }

    private StatsResponse.AchievementInfo createAchievementInfo(Long bifId, Map<EmotionType, Integer> emotionCounts,
                                                               List<StatsResponse.KeywordData> topKeywords) {
        try {
            final int diaryCount = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
            final int streakCount = calculateStreakCount(bifId);
            final List<String> emotions = emotionCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(entry -> entry.getKey().name())
                    .toList();
            final List<String> keywords = topKeywords.stream()
                    .map(StatsResponse.KeywordData::getKeyword)
                    .toList();

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
                .toList();
    }

    private List<StatsResponse.EmotionTrend> createEmotionTrends(Long bifId, LocalDateTime yearMonth) {
        try {
            final LocalDateTime startOfMonth = yearMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            final LocalDateTime endOfMonth = yearMonth.withDayOfMonth(yearMonth.toLocalDate().lengthOfMonth())
                    .withHour(HOUR_23).withMinute(MINUTE_59).withSecond(SECOND_59);

            final List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfMonth, endOfMonth);
            return createEmotionTrendsFromDiaries(monthlyDiaries, yearMonth);

        } catch (Exception e) {
            log.error("감정 트렌드 생성 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    private List<StatsResponse.EmotionTrend> createEmotionTrends(MonthlyDiaryData monthlyData) {
        try {
            return createEmotionTrendsFromDiaries(monthlyData.getDiaries(), monthlyData.getYearMonth());
        } catch (Exception e) {
            log.error("감정 트렌드 생성 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    private List<StatsResponse.EmotionTrend> createEmotionTrendsFromDiaries(List<Diary> monthlyDiaries, LocalDateTime yearMonth) {
        final List<StatsResponse.EmotionTrend> trends = new ArrayList<>();

        if (monthlyDiaries.isEmpty()) {
            return trends;
        }

        for (int day = 1; day <= yearMonth.toLocalDate().lengthOfMonth(); day++) {
            final LocalDateTime dayStart = yearMonth.withDayOfMonth(day).withHour(0).withMinute(0).withSecond(0);
            final LocalDateTime dayEnd = yearMonth.withDayOfMonth(day).withHour(HOUR_23).withMinute(MINUTE_59).withSecond(SECOND_59);

            final List<Diary> dayDiaries = monthlyDiaries.stream()
                    .filter(diary -> !diary.getCreatedAt().isBefore(dayStart) && !diary.getCreatedAt().isAfter(dayEnd))
                    .toList();

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
                final String description = generateTrendDescription(averageScore);

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
    }

    private String determineTrend(double averageScore) {
        if (averageScore >= 1.0) return "상승";
        if (averageScore >= 0.0) return "안정";
        return "하락";
    }

    private String generateTrendDescription(double averageScore) {
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
            
            if (cleanedJson.startsWith("{")) {
                final Map<String, Integer> keywordCounts = objectMapper.readValue(cleanedJson, new TypeReference<Map<String, Integer>>() {});
                return convertKeywordMapToList(keywordCounts);
            }
            
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {});
            
        } catch (Exception e) {
            log.error("키워드 JSON 파싱 실패: {}", json, e);
            return createDefaultTopKeywords();
        }
    }

    private List<Map<String, Object>> convertKeywordMapToList(Map<String, Integer> keywordCounts) {
        final List<Map<String, Object>> keywordList = new ArrayList<>();
        
        if (keywordCounts == null || keywordCounts.isEmpty()) {
            log.info("키워드 데이터가 비어있음 - 빈 리스트 반환");
            return keywordList;
        }
        
        log.info("원본 키워드 빈도수: {}", keywordCounts);
        
        final List<Map.Entry<String, Integer>> sortedKeywords = keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_KEYWORDS)
                .toList();
        
        log.info("정렬된 키워드 Top5: {}", sortedKeywords);
        
        final int maxCount = sortedKeywords.stream()
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(1);
        
        for (int i = 0; i < sortedKeywords.size(); i++) {
            final Map.Entry<String, Integer> entry = sortedKeywords.get(i);
            final Map<String, Object> keywordData = new HashMap<>();
            keywordData.put(KEYWORD_KEY, entry.getKey());
            keywordData.put(COUNT_KEY, entry.getValue());
            keywordData.put(RANK_KEY, i + 1);
            keywordData.put(NORMALIZED_VALUE_KEY, maxCount > 0 ? (double) entry.getValue() / maxCount : 0.0);
            keywordList.add(keywordData);
            
            log.info("키워드 데이터 생성: keyword={}, count={}, rank={}, normalizedValue={}", 
                    entry.getKey(), entry.getValue(), i + 1, 
                    maxCount > 0 ? (double) entry.getValue() / maxCount : 0.0);
        }
        
        log.info("최종 키워드 리스트: {}", keywordList);
        return keywordList;
    }

    private List<Map<String, Object>> createDefaultTopKeywords() {
        return new ArrayList<>();
    }

    private List<StatsResponse.KeywordData> createKeywordDataList(List<Map<String, Object>> topKeywordsData) {
        return topKeywordsData.stream()
                .filter(keyword -> {
                    String keywordText = (String) keyword.get(KEYWORD_KEY);
                    return keywordText != null && !keywordText.trim().isEmpty();
                })
                .map(keyword -> {
                    String keywordText = (String) keyword.get(KEYWORD_KEY);
                    Integer count = (Integer) keyword.get(COUNT_KEY);
                    Integer rank = (Integer) keyword.get(RANK_KEY);
                    
                    Double normalizedValue = keyword.get(NORMALIZED_VALUE_KEY) != null ?
                            (Double) keyword.get(NORMALIZED_VALUE_KEY) : 0.0;
                    
                    return StatsResponse.KeywordData.builder()
                            .keyword(keywordText)
                            .count(count != null ? count : 0)
                            .rank(rank != null ? rank : 1)
                            .normalizedValue(normalizedValue)
                            .build();
                })
                .toList();
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

        if (change > CHANGE_THRESHOLD) {
            return "INCREASE";
        } else if (change < -CHANGE_THRESHOLD) {
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

        if (changePercentage > CHANGE_THRESHOLD) {
            return String.format("지난달보다 %s 감정이 %.1f%% 증가했습니다.", emotion.getKoreanName(), changePercentage);
        } else if (changePercentage < -CHANGE_THRESHOLD) {
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

    public void forceCleanupInvalidKeywords(Long bifId) {
        log.info("=== BIF ID {}의 잘못된 키워드 데이터 강제 정리 시작 ===", bifId);
        
        try {
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            if (existingStats.isPresent()) {
                final Stats stats = existingStats.get();
                
                if (stats.getTopKeywords() != null && stats.getTopKeywords().contains("일상")) {
                    log.warn("잘못된 키워드 데이터 발견 - 강제로 초기화");
                    stats.setTopKeywords("{}"); // 빈 맵으로 초기화
                    statsRepository.save(stats);
                    
                    log.info("잘못된 키워드 데이터 정리 완료");
                } else {
                    log.info("정리할 잘못된 데이터가 없음");
                }
            } else {
                log.info("통계 데이터가 없음");
            }
        } catch (Exception e) {
            log.error("키워드 데이터 정리 중 오류 발생 - bifId: {}", bifId, e);
        }
    }

    public void forceRegenerateStats(Long bifId) {
        log.info("=== BIF ID {}의 통계 데이터 강제 재생성 시작 ===", bifId);
        
        try {
            log.info("캐시 무효화 코드 제거됨");
            
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            if (existingStats.isPresent()) {
                final Stats stats = existingStats.get();
                statsRepository.delete(stats);
                log.info("기존 통계 데이터 삭제 완료");
            }
            
            final List<Diary> monthlyDiaries = diaryRepository.findByUserId(bifId);
            if (!monthlyDiaries.isEmpty()) {
                final Diary latestDiary = monthlyDiaries.stream()
                        .max(Comparator.comparing(Diary::getCreatedAt))
                        .orElse(monthlyDiaries.get(0));
                
                updateStatsWithKeywords(bifId, latestDiary.getContent());
                log.info("새로운 통계 데이터 생성 완료");
            } else {
                log.info("월간 일기가 없음 - 기본 통계 생성");
                generateAndSaveMonthlyStats(bifId, currentYearMonth);
            }
            
            log.info("통계 데이터 강제 재생성 완료");
            
        } catch (Exception e) {
            log.error("통계 데이터 강제 재생성 중 오류 발생 - bifId: {}", bifId, e);
        }
    }
    
    public void cleanupInvalidKeywords(Long bifId) {
        log.info("=== BIF ID {}의 잘못된 키워드 데이터 정리 시작 ===", bifId);
        
        try {
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            if (existingStats.isPresent()) {
                final Stats stats = existingStats.get();
                final Map<String, Integer> currentKeywords = parseTopKeywordsJson(stats.getTopKeywords()).stream()
                        .collect(LinkedHashMap::new, 
                                (map, item) -> map.put((String) item.get(KEYWORD_KEY), (Integer) item.get(COUNT_KEY)), 
                                LinkedHashMap::putAll);
                
                final Map<String, Integer> cleanedKeywords = new HashMap<>();
                for (Map.Entry<String, Integer> entry : currentKeywords.entrySet()) {
                    if (isValidKeyword(entry.getKey())) {
                        cleanedKeywords.put(entry.getKey(), entry.getValue());
                    } else {
                        log.info("잘못된 키워드 제거: {}", entry.getKey());
                    }
                }
                
                stats.setTopKeywords(objectMapper.writeValueAsString(cleanedKeywords));
                statsRepository.save(stats);
                
                log.info("키워드 정리 완료 - 정리 전: {}, 정리 후: {}", currentKeywords.size(), cleanedKeywords.size());
            } else {
                log.info("통계 데이터가 없음");
            }

        } catch (Exception e) {
            log.error("키워드 정리 중 오류 발생 - bifId: {}", bifId, e);
        }
    }
    
    @Override
    public void resetKeywords(Long bifId) {
        try {
            log.info("BIF ID {}의 키워드 데이터 초기화 시작", bifId);
            
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            if (existingStats.isPresent()) {
                final Stats stats = existingStats.get();
                stats.setTopKeywords("{}");
                statsRepository.save(stats);
                log.info("BIF ID {}의 키워드 데이터 초기화 완료", bifId);
            } else {
                log.info("BIF ID {}의 키워드 데이터가 없음", bifId);
            }
            
        } catch (Exception e) {
            log.error("키워드 데이터 초기화 중 오류 발생 - bifId: {}", bifId, e);
        }
    }
    
    private boolean isValidKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        String[] invalidPatterns = {
            "사용불가", "서울역", "우울감", "협회", "회의실", "일상", "일반", "보통", "평범",
            "그냥", "그저", "그런", "이런", "저런", "어떤", "무엇", "언제", "어디", "왜", "어떻게"
        };
        
        for (String pattern : invalidPatterns) {
            if (keyword.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }

    private MonthlyDiaryData loadMonthlyDiaryData(Long bifId, LocalDateTime yearMonth) {
        LocalDateTime startOfMonth = yearMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfMonth = yearMonth.withDayOfMonth(yearMonth.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        
        List<Diary> diaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfMonth, endOfMonth);
        return new MonthlyDiaryData(diaries, yearMonth, bifId);
    }

    private static class MonthlyDiaryData {
        private final List<Diary> diaries;
        private final LocalDateTime yearMonth;
        private final Long bifId;

        public MonthlyDiaryData(List<Diary> diaries, LocalDateTime yearMonth, Long bifId) {
            this.diaries = diaries;
            this.yearMonth = yearMonth;
            this.bifId = bifId;
        }

        public List<Diary> getDiaries() {
            return diaries;
        }

        public LocalDateTime getYearMonth() {
            return yearMonth;
        }

        public Long getBifId() {
            return bifId;
        }
    }

}
