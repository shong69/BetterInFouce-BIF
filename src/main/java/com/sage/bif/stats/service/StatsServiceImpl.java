package com.sage.bif.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.entity.EmotionStatisticsTemplate;
import com.sage.bif.stats.entity.EmotionType;
import com.sage.bif.stats.entity.GuardianAdviceTemplate;
import com.sage.bif.stats.entity.Stats;
import com.sage.bif.stats.repository.EmotionStatisticsTemplateRepository;
import com.sage.bif.stats.repository.GuardianAdviceTemplateRepository;
import com.sage.bif.stats.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import com.sage.bif.stats.event.model.StatsUpdatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.sage.bif.diary.repository.DiaryRepository;
import com.sage.bif.diary.model.Emotion;
import com.sage.bif.stats.util.EmotionMapper;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.user.entity.Bif;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StatsServiceImpl implements StatsService {

    private static final String LABEL_MONTHLY_CHANGE = "감정별 변화";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String NO_ADVICE_MSG = "BIF의 감정 데이터가 없어 조언을 제공할 수 없습니다.";
    private static final String KEYWORD = "keyword";
    private static final String COUNT = "count";

    private final StatsRepository statsRepository;
    private final EmotionStatisticsTemplateRepository templateRepository;
    private final GuardianAdviceTemplateRepository guardianAdviceTemplateRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final DiaryRepository diaryRepository;
    private final BifRepository bifRepository;
    private final ObjectProvider<StatsService> statsServiceProvider;


    @Override
    @Transactional
    public StatsResponse getMonthlyStats(final Long bifId) {
        final LocalDateTime currentYearMonth = getCurrentYearMonth();

        final Optional<Stats> stats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);

        if (stats.isEmpty()) {
            statsServiceProvider.getObject().generateMonthlyStatsAsync(bifId, currentYearMonth);
            return createEmptyStatsResponse(bifId);
        }

        return buildStatsResponseWithRealTimeData(stats.get(), bifId, currentYearMonth);
    }

    @Override
    @Transactional
    public GuardianStatsResponse getGuardianStats(final Long bifId) {
        log.info("보호자가 BIF ID {}의 통계를 조회합니다.", bifId);

        try {
            final StatsResponse bifStats = getMonthlyStatsByBifId(bifId);

            final String bifNickname = getBifNickname(bifId);

            final String advice = getGuardianAdviceFromStats(bifId);

            log.debug("BIF ID {}의 보호자 통계 생성 완료 - 닉네임: {}, 조언 길이: {}",
                    bifId, bifNickname, advice.length());

            return GuardianStatsResponse.builder()
                    .bifNickname(bifNickname)
                    .advice(advice)
                    .emotionRatio(bifStats.getEmotionRatio())
                    .monthlyChange(bifStats.getMonthlyChange())
                    .build();

        } catch (Exception e) {
            log.error("보호자 통계 조회 중 오류 발생 - BIF ID: {}", bifId, e);
            throw new com.sage.bif.stats.exception.StatsProcessingException("보호자 통계 조회 실패", e);
        }
    }

    private StatsResponse getMonthlyStatsByBifId(final Long bifId) {
        final LocalDateTime currentYearMonth = getCurrentYearMonth();

        final Optional<Stats> stats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);

        if (stats.isEmpty()) {
            statsServiceProvider.getObject().generateMonthlyStatsAsync(bifId, currentYearMonth);
            return createEmptyStatsResponse(bifId);
        }

        return buildStatsResponse(stats.get(), bifId, currentYearMonth);
    }

    @Override
    @Transactional
    public void generateMonthlyStats(final Long bifId, final LocalDateTime yearMonth) {
        log.info("BIF ID {}의 {}년 {}월 감정 분석 텍스트 생성 시작", bifId, yearMonth.getYear(), yearMonth.getMonthValue());

        try {
            final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, yearMonth);
            final String statisticsText = generateStatisticsTextFromTemplate(emotionCounts);
            final String guardianAdvice = generateGuardianAdviceFromTemplate(emotionCounts);

            final String emotionCountsJson = objectMapper.writeValueAsString(emotionCounts);

            final Optional<Stats> existing = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, yearMonth);

            final Stats statsToSave = existing.orElseGet(() -> Stats.builder()
                    .bifId(bifId)
                    .yearMonth(yearMonth)
                    .build());

            statsToSave.setEmotionStatisticsText(statisticsText);
            statsToSave.setGuardianAdviceText(guardianAdvice);
            statsToSave.setEmotionCounts(emotionCountsJson);

            final Stats savedStats = statsRepository.save(statsToSave);

            eventPublisher.publishEvent(new StatsUpdatedEvent(this, savedStats, bifId, existing.isPresent() ? "UPDATE" : "CREATE", "월별 감정 분석 텍스트 생성"));

            log.info("BIF ID {}의 {}년 {}월 감정 분석 텍스트 생성 완료", bifId, yearMonth.getYear(), yearMonth.getMonthValue());

        } catch (Exception e) {
            log.error("BIF ID {}의 {}년 {}월 감정 분석 텍스트 생성 중 오류 발생: {}",
                    bifId, yearMonth.getYear(), yearMonth.getMonthValue(), e.getMessage(), e);
        }
    }

    @Override
    public void updateStatsWithKeywords(final Long bifId, final String diaryContent) {
        log.info("BIF ID {}의 키워드 기반 통계 업데이트 시작", bifId);
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
            log.error("BIF ID {}의 실시간 통계 갱신 중 오류 발생: {}", bifId, e.getMessage(), e);
        }
    }

    @Async
    @Transactional
    public void generateMonthlyStatsAsync(final Long bifId, final LocalDateTime yearMonth) {
        try {
            statsServiceProvider.getObject().generateMonthlyStats(bifId, yearMonth);
        } catch (Exception e) {
            log.error("BIF ID {}의 {}년 {}월 통계 생성 중 오류 발생: {}",
                    bifId, yearMonth.getYear(), yearMonth.getMonthValue(), e.getMessage(), e);
        }
    }

    private StatsResponse createEmptyStatsResponse(final Long bifId) {
        try {
            final ProfileMeta meta = loadProfileMeta(bifId);
            return buildEmptyStatsResponse(bifId, meta.nickname, meta.joinDate, meta.totalDiaryCount, meta.connectionCode);
        } catch (Exception e) {
            log.error("Failed to create empty stats response for BIF ID: {}. Error: {}", bifId, e.getMessage());
            return buildEmptyStatsResponse(bifId, "BIF", LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT)), 0, "");
        }
    }

    private StatsResponse buildEmptyStatsResponse(final Long bifId, final String nickname, final String joinDate, 
                                                final int totalDiaryCount, final String connectionCode) {
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
                    .build();
    }

    private record ProfileMeta(String nickname, String joinDate, String connectionCode, int totalDiaryCount) {
    }

    private ProfileMeta loadProfileMeta(final Long bifId) {
        final var bifOpt = bifRepository.findById(bifId);
        final String nickname = bifOpt.map(Bif::getNickname).orElse("BIF");
        final String joinDate = bifOpt.map(bif -> bif.getCreatedAt().format(DateTimeFormatter.ofPattern(DATE_FORMAT))).orElse("");
        final String connectionCode = bifOpt.map(Bif::getConnectionCode).orElse("");
        final int totalDiaryCount = calculateTotalDiaryCount(bifId);
        return new ProfileMeta(nickname, joinDate, connectionCode, totalDiaryCount);
    }

    private Map<EmotionType, Integer> calculateEmotionCounts(final Long bifId, final LocalDateTime yearMonth) {
        log.debug("BIF ID {}의 {}년 {}월 감정 데이터 계산 시작", bifId, yearMonth.getYear(), yearMonth.getMonthValue());

        LocalDateTime startOfMonth = yearMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = yearMonth.withDayOfMonth(yearMonth.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfMonth, endOfMonth);

        if (monthlyDiaries.isEmpty()) {
            log.debug("BIF ID {}의 {}년 {}월에 작성된 일기가 없습니다.", bifId, yearMonth.getYear(), yearMonth.getMonthValue());
            return initializeEmotionCounts();
        }

        Map<EmotionType, Integer> emotionCounts = initializeEmotionCounts();

        for (Diary diary : monthlyDiaries) {
            Emotion diaryEmotion = diary.getEmotion();
            EmotionType statsEmotion = EmotionMapper.mapDiaryEmotionToStats(diaryEmotion);

            emotionCounts.put(statsEmotion, emotionCounts.get(statsEmotion) + 1);
        }

        log.debug("BIF ID {}의 {}년 {}월 감정 데이터 계산 완료: {}",
                bifId, yearMonth.getYear(), yearMonth.getMonthValue(), emotionCounts);

        return emotionCounts;
    }

    private void saveEmotionCountsToStats(final Long bifId, final LocalDateTime yearMonth, final Map<EmotionType, Integer> emotionCounts) {
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
            log.error("감정 카운트 저장 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private List<StatsResponse.EmotionRatio> calculateEmotionRatio(final Map<EmotionType, Integer> emotionCounts) {
        final int totalCount = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (totalCount == 0) {
            return Collections.emptyList();
        }

        final List<StatsResponse.EmotionRatio> emotionRatios = new ArrayList<>();

        for (final Map.Entry<EmotionType, Integer> entry : emotionCounts.entrySet()) {
            final EmotionType emotionType = entry.getKey();
            final Integer count = entry.getValue();

            final StatsResponse.EmotionRatio ratio = StatsResponse.EmotionRatio.builder()
                    .emotion(emotionType)
                    .value(count)
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

    private int getEmotionOrder(final EmotionType emotionType) {
        return switch (emotionType) {
            case GREAT -> 1;
            case GOOD -> 2;
            case OKAY -> 3;
            case DOWN -> 4;
            case ANGRY -> 5;
        };
    }

    private Map<EmotionType, Integer> initializeEmotionCounts() {
        final Map<EmotionType, Integer> counts = new EnumMap<>(EmotionType.class);
        for (EmotionType emotion : EmotionType.values()) {
            counts.put(emotion, 0);
        }
        return counts;
    }

    private Map<EmotionType, Integer> parseEmotionCountsJson(final String json) {
        if (json == null || json.trim().isEmpty()) {
            log.debug("Empty or null emotion counts JSON, returning empty map");
            return new EnumMap<>(EmotionType.class);
        }

        try {
            final String cleanedJson = unwrapIfQuoted(json.trim());
            log.debug("Parsing emotion counts JSON: {}", cleanedJson);
            
            Map<String, Integer> rawCounts = objectMapper.readValue(cleanedJson, new TypeReference<>() {});
            
            return processEmotionCountsFromRaw(rawCounts);
        } catch (Exception e) {
            log.error("Failed to parse emotion counts JSON: '{}'. Error: {}", json, e.getMessage());
            return new EnumMap<>(EmotionType.class);
        }
    }

    private Map<EmotionType, Integer> processEmotionCountsFromRaw(final Map<String, Integer> rawCounts) {
            Map<EmotionType, Integer> emotionCounts = new EnumMap<>(EmotionType.class);
            for (EmotionType emotion : EmotionType.values()) {
                emotionCounts.put(emotion, 0);
            }
            
            for (Map.Entry<String, Integer> entry : rawCounts.entrySet()) {
                try {
                    EmotionType emotionType = mapDbeaverEmotionToStatsEmotion(entry.getKey());
                    emotionCounts.put(emotionType, entry.getValue());
                    log.debug("Mapped {} -> {} with count {}", entry.getKey(), emotionType, entry.getValue());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown emotion type found in JSON: {}", entry.getKey());
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
            default -> {
                log.warn("Unknown emotion type: {}, defaulting to OKAY", dbeaverEmotion);
                yield EmotionType.OKAY;
        }
        };
    }


    private List<Map<String, Object>> parseTopKeywordsJson(final String json) {
        if (json == null || json.trim().isEmpty()) {
            log.debug("Empty or null top keywords JSON, returning empty list");
            return new ArrayList<>();
        }
        try {
            final String cleanedJson = unwrapIfQuoted(json.trim());
            log.debug("Parsing top keywords JSON: {}", cleanedJson);

            if (cleanedJson.startsWith("{") && cleanedJson.endsWith("}")) {
                return parseTopKeywordsFromObject(cleanedJson);
            } else if (cleanedJson.startsWith("[") && cleanedJson.endsWith("]")) {
                return parseTopKeywordsFromArray(cleanedJson);
            } else {
                return objectMapper.readValue(cleanedJson, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.error("Failed to parse top keywords JSON: '{}'. Error: {}", json, e.getMessage());
            return createDefaultTopKeywords();
        }
    }

    private List<Map<String, Object>> parseTopKeywordsFromObject(final String cleanedJson) {
        try {
                Map<String, Integer> keywordMap = objectMapper.readValue(cleanedJson, new TypeReference<>() {});
                List<Map<String, Object>> result = new ArrayList<>();
                
                List<Map.Entry<String, Integer>> sortedEntries = keywordMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();
                
                for (int i = 0; i < Math.min(5, sortedEntries.size()); i++) {
                    Map.Entry<String, Integer> entry = sortedEntries.get(i);
                    Map<String, Object> keywordData = new HashMap<>();
                keywordData.put(KEYWORD, entry.getKey());
                keywordData.put(COUNT, entry.getValue());
                    keywordData.put("rank", i + 1);
                    result.add(keywordData);
                }
                
                return result;
        } catch (Exception e) {
            log.error("Failed to parse top keywords from object: {}", cleanedJson, e);
            throw new com.sage.bif.stats.exception.StatsParsingException("Failed to parse top keywords from object", e);
        }
    }

    private List<Map<String, Object>> parseTopKeywordsFromArray(final String cleanedJson) {
        try {
                List<Map<String, Object>> keywords = objectMapper.readValue(cleanedJson, new TypeReference<>() {});
                List<Map<String, Object>> result = new ArrayList<>();

                for (int i = 0; i < keywords.size(); i++) {
                    Map<String, Object> keywordData = keywords.get(i);
                if (keywordData.containsKey(KEYWORD) && keywordData.containsKey(COUNT)) {
                        keywordData.put("rank", i + 1);
                        result.add(keywordData);
                    }
                }

                return result;
        } catch (Exception e) {
            log.error("Failed to parse top keywords from array: {}", cleanedJson, e);
            throw new com.sage.bif.stats.exception.StatsParsingException("Failed to parse top keywords from array", e);
        }
    }

    private List<Map<String, Object>> createDefaultTopKeywords() {
        List<Map<String, Object>> defaultKeywords = new ArrayList<>();
        String[] defaultWords = {"가족", "직장", "친구", "휴식", "건강"};

        for (int i = 0; i < defaultWords.length; i++) {
            Map<String, Object> keywordData = new HashMap<>();
            keywordData.put(KEYWORD, defaultWords[i]);
            keywordData.put(COUNT, 0);
            keywordData.put("rank", i + 1);
            defaultKeywords.add(keywordData);
        }

        return defaultKeywords;
    }

    private int calculateTotalDiaryCount(final Long bifId) {
        try {
            LocalDateTime startOfYear = LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfYear = LocalDateTime.now().withMonth(12).withDayOfMonth(31).withHour(23).withMinute(59).withSecond(59);
            
            List<Diary> yearlyDiaries = diaryRepository.findByUserIdAndDateBetween(bifId, startOfYear, endOfYear);
            return yearlyDiaries.size();
        } catch (Exception e) {
            log.error("Failed to calculate total diary count for BIF ID: {}. Error: {}", bifId, e.getMessage());
            return 0;
        }
    }

    private String generateStatisticsTextFromTemplate(final Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return "이번 달에는 감정 데이터가 없습니다.";
        }

        final Map<EmotionType, Double> ratios = emotionCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (double) entry.getValue() / total * 100
                ));

        final EmotionType dominantEmotion = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(EmotionType.OKAY);

        final double dominantRatio = ratios.get(dominantEmotion);

        final String okayRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.OKAY));
        final String goodRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.GOOD));
        final String angryRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.ANGRY));
        final String downRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.DOWN));
        final String greatRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.GREAT));

        final Optional<EmotionStatisticsTemplate> template = templateRepository.findByEmotionRanges(okayRange, goodRange, angryRange, downRange, greatRange);

        if (template.isPresent()) {
            log.debug("Found emotion statistics template for ranges: okay={}, good={}, angry={}, down={}, great={}",
                    okayRange, goodRange, angryRange, downRange, greatRange);
            return template.get().getStatisticsText();
        }

        log.warn("No emotion statistics template found for ranges: okay={}, good={}, angry={}, down={}, great={}. Using fallback statistics.",
                okayRange, goodRange, angryRange, downRange, greatRange);

        return generateStatisticsBasedStatisticsText(ratios, dominantEmotion, dominantRatio);
    }

    private String getRangeForPercentage(final double percentage) {
        if (percentage <= 30.0) {
            return "0-30";
        } else if (percentage <= 60.0) {
            return "31-60";
        } else {
            return "61-100";
        }
    }

    private String generateStatisticsBasedStatisticsText(final Map<EmotionType, Double> ratios,
                                                         final EmotionType dominantEmotion,
                                                         final double dominantRatio) {
        final StringBuilder statistics = new StringBuilder();

        statistics.append(String.format("이번 달에는 %s한 감정이 %.1f%%로 가장 많이 나타났습니다. ",
                getEmotionKoreanName(dominantEmotion), dominantRatio));

        final double positiveEmotionRatio = getEmotionRatio(ratios, EmotionType.GOOD) + getEmotionRatio(ratios, EmotionType.GREAT);
        final double negativeEmotionRatio = getEmotionRatio(ratios, EmotionType.ANGRY) + getEmotionRatio(ratios, EmotionType.DOWN);

        if (positiveEmotionRatio > 50.0) {
            statistics.append("전반적으로 긍정적인 감정을 많이 느끼셨네요!");
        } else if (negativeEmotionRatio > 30.0) {
            statistics.append("부정적인 감정이 다소 높게 나타났습니다. ");
            if (getEmotionRatio(ratios, EmotionType.DOWN) > 20.0) {
                statistics.append("우울한 감정이 많았던 것 같아요. ");
            }
            if (getEmotionRatio(ratios, EmotionType.ANGRY) > 20.0) {
                statistics.append("화난 감정도 자주 느끼셨네요. ");
            }
            statistics.append("힘든 일이 있었나요?");
        } else {
            statistics.append("감정이 비교적 안정적으로 유지되었습니다.");
        }

        return statistics.toString();
    }

    private double getEmotionRatio(final Map<EmotionType, Double> ratios, final EmotionType emotionType) {
        return ratios.getOrDefault(emotionType, 0.0);
    }

    private String getEmotionKoreanName(final EmotionType emotionType) {
        return switch (emotionType) {
            case OKAY -> "평범";
            case GOOD -> "좋은";
            case ANGRY -> "화난";
            case DOWN -> "우울한";
            case GREAT -> "최고의";
        };
    }

    private List<StatsResponse.MonthlyChange> getMonthlyChange(final Long bifId, final LocalDateTime yearMonth) {
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
                int orderA = getEmotionOrder(a.getEmotion());
                int orderB = getEmotionOrder(b.getEmotion());
                return Integer.compare(orderA, orderB);
            });

        } catch (Exception e) {
            log.error("월별 감정 비교 데이터 생성 중 오류 발생 - bifId: {}, yearMonth: {}", bifId, yearMonth, e);
            addDefaultMonthlyChangeItems(monthlyChange);
        }

        return monthlyChange;
    }

    private StatsResponse.MonthlyChange buildMonthlyComparisonItem(final EmotionType emotion,
                                                                   final Integer currentValue,
                                                                   final Integer previousValue) {
        final Double changePercentage = calculateChangePercentage(previousValue, currentValue);
        final String changeStatus = determineChangeStatus(currentValue, previousValue);
        final String changeDescription = generateChangeDescription(emotion, currentValue, previousValue, changePercentage);

        return StatsResponse.MonthlyChange.builder()
                .month(LABEL_MONTHLY_CHANGE)
                .emotion(emotion)
                .value(currentValue)
                .previousValue(previousValue)
                .changePercentage(changePercentage)
                .changeStatus(changeStatus)
                .changeDescription(changeDescription)
                .build();
    }

    private String determineChangeStatus(final Integer currentValue, final Integer previousValue) {
        if (previousValue == 0) {
            return currentValue > 0 ? "NEW" : "SAME";
        }

        double change = ((double) (currentValue - previousValue) / previousValue) * 100.0;

        if (change > 20.0) {
            return "INCREASE";
        } else if (change < -20.0) {
            return "DECREASE";
        } else {
            return "STABLE";
        }
    }

    private String generateChangeDescription(final EmotionType emotion,
                                             final Integer currentValue,
                                             final Integer previousValue,
                                             final Double changePercentage) {
        if (previousValue == 0) {
            if (currentValue > 0) {
                return String.format("지난달에는 없었던 %s 감정이 이번달에 %d회 나타났습니다.",
                        getEmotionKoreanName(emotion), currentValue);
            }
            return String.format("지난달과 이번달 모두 %s 감정이 나타나지 않았습니다.",
                    getEmotionKoreanName(emotion));
        }

        if (changePercentage > 20.0) {
            return String.format("지난달보다 %s 감정이 %.1f%% 증가했습니다.",
                    getEmotionKoreanName(emotion), changePercentage);
        } else if (changePercentage < -20.0) {
            return String.format("지난달보다 %s 감정이 %.1f%% 감소했습니다.",
                    getEmotionKoreanName(emotion), Math.abs(changePercentage));
        } else {
            return String.format("지난달과 비슷한 수준의 %s 감정을 보였습니다.",
                    getEmotionKoreanName(emotion));
        }
    }

    private Map<EmotionType, Integer> parseEmotionCountsFromStats(final Optional<Stats> statsOpt) {
        if (statsOpt.isPresent() && statsOpt.get().getEmotionCounts() != null && !statsOpt.get().getEmotionCounts().trim().isEmpty()) {
            try {
                final String countsJson = unwrapIfQuoted(statsOpt.get().getEmotionCounts());
                log.debug("Parsing emotion counts from stats: {}", countsJson);
                
                Map<String, Integer> rawCounts = objectMapper.readValue(countsJson, new TypeReference<>() {});
                
                return processEmotionCountsFromRaw(rawCounts);
            } catch (Exception e) {
                log.error("Failed to parse emotion counts from stats: {}. Error: {}", statsOpt.get().getEmotionCounts(), e.getMessage());
                return new EnumMap<>(EmotionType.class);
            }
        }
        return new EnumMap<>(EmotionType.class);
    }



    private List<StatsResponse.EmotionRatio> calculateEmotionRatioFromStats(final Stats statsData) {
        try {
            if (statsData.getEmotionCounts() != null && !statsData.getEmotionCounts().trim().isEmpty()) {
                final String countsJson = unwrapIfQuoted(statsData.getEmotionCounts());
                log.debug("Calculating emotion ratio from stats: {}", countsJson);
                
                Map<String, Integer> rawCounts = objectMapper.readValue(countsJson, new TypeReference<>() {});
                
                Map<EmotionType, Integer> emotionCounts = processEmotionCountsFromRaw(rawCounts);
                
                return calculateEmotionRatio(emotionCounts);
            }
        } catch (Exception e) {
            log.error("Failed to calculate emotion ratio from stats: {}. Error: {}", statsData.getEmotionCounts(), e.getMessage());
        }
        
        return calculateEmotionRatio(initializeEmotionCounts());
    }

    private void addDefaultMonthlyChangeItems(final List<StatsResponse.MonthlyChange> monthlyChange) {
        for (EmotionType emotion : EmotionType.values()) {
            monthlyChange.add(StatsResponse.MonthlyChange.builder()
                    .month(LABEL_MONTHLY_CHANGE)
                    .emotion(emotion)
                    .value(0)
                    .previousValue(0)
                    .changePercentage(0.0)
                    .build());
        }
    }

    private Double calculateChangePercentage(final Integer previousValue, final Integer currentValue) {
        if (previousValue == 0) {
            return currentValue > 0 ? 100.0 : 0.0;
        }
        return ((double) (currentValue - previousValue) / previousValue) * 100.0;
    }

    private String getBifNickname(final Long bifId) {
        if (bifId == null) {
            log.warn("BIF ID is null");
            return "BIF";
        }

        try {
            final Bif bif = bifRepository.findById(bifId)
                    .orElseThrow(() -> new RuntimeException("BIF not found: " + bifId));

            final String nickname = bif.getNickname();
            log.debug("BIF ID {}의 닉네임 조회 완료: {}", bifId, nickname);
            return nickname;

        } catch (Exception e) {
            log.error("BIF 닉네임 조회 중 오류 발생 - BIF ID: {}", bifId, e);
            return "BIF_" + bifId;
        }
    }


    private List<StatsResponse.KeywordData> createKeywordDataList(final List<Map<String, Object>> topKeywordsData) {
        return topKeywordsData.stream()
                .map(keyword -> StatsResponse.KeywordData.builder()
                        .keyword((String) keyword.get(KEYWORD))
                        .count((Integer) keyword.get(COUNT))
                        .rank(topKeywordsData.indexOf(keyword) + 1)
                        .build())
                .toList();
    }

    private String generateFallbackGuardianAdvice(final Map<EmotionType, Double> ratios) {
        final double positiveRatio = getEmotionRatio(ratios, EmotionType.GOOD) + getEmotionRatio(ratios, EmotionType.GREAT);
        final double negativeRatio = getEmotionRatio(ratios, EmotionType.ANGRY) + getEmotionRatio(ratios, EmotionType.DOWN);

        if (positiveRatio > 60.0) {
            return "BIF가 매우 긍정적인 감정을 많이 느끼고 있습니다. 이런 좋은 기분을 유지할 수 있도록 지지해주세요.";
        } else if (negativeRatio > 40.0) {
            return "BIF가 부정적인 감정을 많이 경험하고 있습니다. 따뜻한 관심과 대화를 통해 도움을 주세요.";
        } else {
            return "BIF가 균형잡힌 감정 상태를 유지하고 있습니다. 이런 안정적인 상태를 지지해주세요.";
        }
    }

    private LocalDateTime getCurrentYearMonth() {
        final LocalDateTime now = LocalDateTime.now();
        return LocalDateTime.of(now.getYear(), now.getMonth(), 1, 0, 0, 0);
    }

    private StatsResponse buildStatsResponse(final Stats statsData, final Long bifId, final LocalDateTime yearMonth) {
        try {
            Map<EmotionType, Integer> emotionCounts = parseEmotionCountsJson(statsData.getEmotionCounts());

            List<StatsResponse.EmotionRatio> emotionRatio = calculateEmotionRatio(emotionCounts);

            List<StatsResponse.KeywordData> topKeywords = createKeywordDataList(
                    parseTopKeywordsJson(statsData.getTopKeywords()));

            List<StatsResponse.MonthlyChange> monthlyChange = getMonthlyChange(bifId, yearMonth);

            return StatsResponse.builder()
                    .statisticsText(statsData.getEmotionStatisticsText())
                    .emotionRatio(emotionRatio)
                    .topKeywords(topKeywords)
                    .monthlyChange(monthlyChange)
                    .build();

        } catch (Exception e) {
            log.error("통계 응답 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new com.sage.bif.stats.exception.StatsProcessingException("통계 응답 생성 실패", e);
        }
    }

    private StatsResponse buildStatsResponseWithRealTimeData(final Stats statsData, final Long bifId, final LocalDateTime yearMonth) {
        try {
            
            List<StatsResponse.EmotionRatio> emotionRatio = calculateEmotionRatioFromStats(statsData);
            
            List<StatsResponse.KeywordData> topKeywords = createKeywordDataList(
                    parseTopKeywordsJson(statsData.getTopKeywords()));

            List<StatsResponse.MonthlyChange> monthlyChange = getMonthlyChange(bifId, yearMonth);

            final ProfileMeta meta = loadProfileMeta(bifId);
            return buildStatsResponseWithData(statsData, bifId, emotionRatio, topKeywords, monthlyChange, meta);

        } catch (Exception e) {
            log.error("통계 응답 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new com.sage.bif.stats.exception.StatsProcessingException("통계 응답 생성 실패", e);
        }
    }

    private String unwrapIfQuoted(final String jsonLike) {
        if (jsonLike == null || jsonLike.isEmpty()) return jsonLike;
        final String trimmed = jsonLike.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            String result = trimmed.substring(1, trimmed.length() - 1);
            result = result.replace("\\\"", "\"");
            return result;
        }
        return trimmed;
    }

    private String getGuardianAdviceFromStats(final Long bifId) {
        final LocalDateTime currentYearMonth = getCurrentYearMonth();

        try {
            final Optional<Stats> stats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);

            if (stats.isPresent() && stats.get().getGuardianAdviceText() != null &&
                    !stats.get().getGuardianAdviceText().trim().isEmpty()) {

                log.debug("BIF ID {}의 저장된 보호자 조언을 사용합니다.", bifId);
                return stats.get().getGuardianAdviceText();
            }

            log.debug("BIF ID {}의 보호자 조언을 새로 생성합니다.", bifId);
            final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, currentYearMonth);
            return generateGuardianAdviceFromTemplate(emotionCounts);

        } catch (Exception e) {
            log.error("보호자 조언 조회 중 오류 발생 - BIF ID: {}", bifId, e);
            return "BIF의 감정 상태를 확인할 수 없어 조언을 제공할 수 없습니다.";
        }
    }

    private String generateGuardianAdviceFromTemplate(final Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return NO_ADVICE_MSG;
        }

        final Map<EmotionType, Double> ratios = emotionCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (double) entry.getValue() / total * 100
                ));

        final String okayRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.OKAY));
        final String goodRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.GOOD));
        final String angryRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.ANGRY));
        final String downRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.DOWN));
        final String greatRange = getRangeForPercentage(getEmotionRatio(ratios, EmotionType.GREAT));

        final Optional<GuardianAdviceTemplate> adviceTemplate = guardianAdviceTemplateRepository.findByEmotionRanges(
                okayRange, goodRange, angryRange, downRange, greatRange);

        if (adviceTemplate.isPresent()) {
            log.debug("Found guardian advice template for ranges: okay={}, good={}, angry={}, down={}, great={}",
                    okayRange, goodRange, angryRange, downRange, greatRange);
            return adviceTemplate.get().getAdviceText();
        }

        log.warn("No guardian advice template found for ranges: okay={}, good={}, angry={}, down={}, great={}. Using fallback advice.",
                okayRange, goodRange, angryRange, downRange, greatRange);

        return generateFallbackGuardianAdvice(ratios);
    }

    private StatsResponse buildStatsResponseWithData(final Stats statsData, final Long bifId, 
                                                   final List<StatsResponse.EmotionRatio> emotionRatio,
                                                   final List<StatsResponse.KeywordData> topKeywords,
                                                   final List<StatsResponse.MonthlyChange> monthlyChange,
                                                   final ProfileMeta meta) {
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
                .build();
    }

}
