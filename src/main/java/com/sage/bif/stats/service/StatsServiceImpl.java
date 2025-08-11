package com.sage.bif.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.entity.EmotionStatisticsTemplate;
import com.sage.bif.stats.entity.EmotionType;
import com.sage.bif.stats.entity.GuardianAdviceTemplate;
import com.sage.bif.stats.entity.Stats;
import com.sage.bif.stats.exception.StatsDataParseException;
import com.sage.bif.stats.repository.EmotionStatisticsTemplateRepository;
import com.sage.bif.stats.repository.GuardianAdviceTemplateRepository;
import com.sage.bif.stats.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import com.sage.bif.stats.event.model.StatsUpdatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;


//todo: 일부 기능 사용자화 필요
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StatsServiceImpl implements StatsService, ApplicationContextAware {

    private static final String LABEL_MONTHLY_CHANGE = "감정별 변화";

    private final StatsRepository statsRepository;
    private final EmotionStatisticsTemplateRepository templateRepository;
    private final GuardianAdviceTemplateRepository guardianAdviceTemplateRepository;
    private final ApplicationEventPublisher eventPublisher; // 추가
    private final ObjectMapper objectMapper;
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public StatsResponse getMonthlyStats(final Long bifId) {
        final Integer year = getCurrentYear();
        final Integer month = getCurrentMonth();
        
        final Optional<Stats> stats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        if (stats.isEmpty()) {
            applicationContext.getBean(StatsServiceImpl.class).generateMonthlyStats(bifId, year, month);
            return applicationContext.getBean(StatsServiceImpl.class).getMonthlyStats(bifId);
        }

        return buildStatsResponse(stats.get(), bifId, year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianStatsResponse getGuardianStats(final Long bifId) {
        // Guardian은 연결된 BIF의 통계 조회
        final StatsResponse bifStats = getMonthlyStatsByBifId(bifId);
        
        final String bifNickname = getBifNickname(bifId);
        final String advice = getGuardianAdviceFromStats(bifId);

        return GuardianStatsResponse.builder()
                .bifNickname(bifNickname)
                .advice(advice)
                .emotionRatio(bifStats.getEmotionRatio())
                .monthlyChange(bifStats.getMonthlyChange())
                .build();
    }
    
    private StatsResponse getMonthlyStatsByBifId(final Long bifId) {
        final Integer year = getCurrentYear();
        final Integer month = getCurrentMonth();
        
        final Optional<Stats> stats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        if (stats.isEmpty()) {
            applicationContext.getBean(StatsServiceImpl.class).generateMonthlyStats(bifId, year, month);
            return getMonthlyStatsByBifId(bifId);
        }

        return buildStatsResponse(stats.get(), bifId, year, month);
    }

    @Override
    @Transactional
    public void generateMonthlyStats(final Long bifId, final Integer year, final Integer month) {
        final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, year, month);
        final String statisticsText = generateStatisticsTextFromTemplate(emotionCounts);
        final String guardianAdvice = generateGuardianAdviceFromTemplate(emotionCounts);
    
        final Stats stats = Stats.builder()
                .bifId(bifId)
                .year(year)
                .month(month)
                .emotionStatisticsText(statisticsText)
                .guardianAdviceText(guardianAdvice)
                .emotionCounts(null) 
                .topKeywords(null)
                .build();
    
        final Stats savedStats = statsRepository.save(stats);
        
        // 이벤트 발행
        eventPublisher.publishEvent(new StatsUpdatedEvent(this, savedStats, bifId, "CREATE", "월별 통계 생성"));
        
        log.info("BIF ID {}의 {}년 {}월 통계 생성 완료", bifId, year, month);
    }
    
    @Override
    public void generateMonthlyEmotionStatistics(final Long bifId, final Integer year, final Integer month) {
        final Optional<Stats> existingStats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        if (existingStats.isPresent()) {
            final Stats stats = existingStats.get();
            final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, year, month);
            final String statisticsText = generateStatisticsTextFromTemplate(emotionCounts);
            final String guardianAdvice = generateGuardianAdviceFromTemplate(emotionCounts);
            
            stats.setEmotionStatisticsText(statisticsText);
            stats.setGuardianAdviceText(guardianAdvice);
            final Stats updatedStats = statsRepository.save(stats);
            
            eventPublisher.publishEvent(new StatsUpdatedEvent(this, updatedStats, bifId, "UPDATE", "감정 통계 텍스트 업데이트"));
            
            log.info("BIF ID {}의 {}년 {}월 감정 통계 텍스트와 보호자 조언 업데이트 완료", bifId, year, month);
        } else {
            log.warn("BIF ID {}의 {}년 {}월 통계 데이터가 없어 전체 통계를 생성합니다.", bifId, year, month);
            applicationContext.getBean(StatsServiceImpl.class).generateMonthlyStats(bifId, year, month);
        }
    }

    private Map<EmotionType, Integer> calculateEmotionCounts(final Long bifId, final Integer year, final Integer month) {
        final Optional<com.sage.bif.stats.entity.Stats> statsOptional =
                statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);

        final Map<EmotionType, Integer> counts = initializeEmotionCounts();

        if (statsOptional.isEmpty()) {
            return counts;
        }

        final com.sage.bif.stats.entity.Stats stat = statsOptional.get();
        if (stat.getEmotionCounts() == null) {
            log.debug("No emotion counts data available for BIF ID {} in {}년 {}월", bifId, year, month);
            return counts;
        }

        try {
            final Map<String, Integer> rawCounts = parseEmotionCountsJson(unwrapIfQuoted(stat.getEmotionCounts()));
            mergeEmotionCounts(rawCounts, counts, bifId);
            return counts;
        } catch (JsonMappingException e) {
            log.error("JSON mapping error parsing emotion counts for BIF ID {}: {}", bifId, e.getMessage());
            throw new StatsDataParseException("감정 데이터 파싱 중 매핑 오류가 발생했습니다.", e);
        } catch (JsonProcessingException e) {
            log.error("JSON processing error parsing emotion counts for BIF ID {}: {}", bifId, e.getMessage());
            throw new StatsDataParseException("감정 데이터 파싱 중 처리 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("Unexpected error parsing emotion counts for BIF ID {}: {}", bifId, e.getMessage());
            throw new StatsDataParseException("감정 데이터 파싱 중 예상치 못한 오류가 발생했습니다.", e);
        }
    }

    private Map<EmotionType, Integer> initializeEmotionCounts() {
        final Map<EmotionType, Integer> counts = new EnumMap<>(EmotionType.class);
        for (EmotionType emotion : EmotionType.values()) {
            counts.put(emotion, 0);
        }
        return counts;
    }

    private Map<String, Integer> parseEmotionCountsJson(final String json) throws JsonProcessingException {
        if (json == null || json.trim().isEmpty()) {
            log.debug("Empty or null emotion counts JSON, returning empty map");
            return new HashMap<>();
        }
        
        try {
            final String cleanedJson = unwrapIfQuoted(json.trim());
            log.debug("Parsing emotion counts JSON: {}", cleanedJson);
            return objectMapper.readValue(cleanedJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            log.error("Failed to parse emotion counts JSON: '{}'. Error: {}", json, e.getMessage());
            throw new JsonProcessingException("Failed to parse emotion counts JSON", e) {};
        }
    }

    private void mergeEmotionCounts(final Map<String, Integer> rawCounts,
                                    final Map<EmotionType, Integer> counts,
                                    final Long bifId) {
        for (Map.Entry<String, Integer> entry : rawCounts.entrySet()) {
            final EmotionType emotionType = toEmotionTypeOrNull(entry.getKey());
            if (emotionType == null) {
                log.warn("Unknown emotion type found in stats: {} for BIF ID {}", entry.getKey(), bifId);
                continue;
            }
            counts.put(emotionType, counts.get(emotionType) + entry.getValue());
        }
    }

    private EmotionType toEmotionTypeOrNull(final String key) {
        try {
            return EmotionType.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<Map<String, Object>> calculateTopKeywords(final Long bifId, final Integer year, final Integer month) {
        final Optional<com.sage.bif.stats.entity.Stats> statsOptional = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        if (statsOptional.isPresent()) {
            final com.sage.bif.stats.entity.Stats stat = statsOptional.get();
            try {
                if (stat.getTopKeywords() != null && !stat.getTopKeywords().trim().isEmpty()) {
                    final String cleanedJson = unwrapIfQuoted(stat.getTopKeywords().trim());
                    log.debug("Parsing top keywords for BIF {}: {}", bifId, cleanedJson);
                    final List<Map<String, Object>> keywords = objectMapper.readValue(cleanedJson, new TypeReference<List<Map<String, Object>>>() {});
                    log.debug("Retrieved {} keywords from stats for BIF ID {}: {}", keywords.size(), bifId, keywords);
                    return keywords;
                } else {
                    log.debug("No top keywords data available for BIF ID {} in {}년 {}월", bifId, year, month);
                    return new ArrayList<>();
                }
            } catch (JsonMappingException e) {
                log.error("JSON mapping error parsing top keywords for BIF ID {}: {}", bifId, e.getMessage());
                return new ArrayList<>();
            } catch (JsonProcessingException e) {
                log.error("JSON processing error parsing top keywords for BIF ID {}: {}", bifId, e.getMessage());
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("Unexpected error parsing top keywords for BIF ID {}: {}", bifId, e.getMessage());
                return new ArrayList<>();
            }
        }

        log.debug("No stats found for BIF ID {} in {}년 {}월", bifId, year, month);
        return new ArrayList<>();
    }



    private String generateStatisticsTextFromTemplate(final Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return "이번 달에는 감정 데이터가 없습니다.";
        }

        final Map<EmotionType, Double> ratios = emotionCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (double) entry.getValue() / total * 100
                ));

        final EmotionType dominantEmotion = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(EmotionType.OKAY);

        final double dominantRatio = ratios.get(dominantEmotion);
        
        final String okayRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.OKAY, 0.0));
        final String goodRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.GOOD, 0.0));
        final String angryRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.ANGRY, 0.0));
        final String downRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.DOWN, 0.0));
        final String greatRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.GREAT, 0.0));

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
        
        final double positiveEmotionRatio = ratios.getOrDefault(EmotionType.GOOD, 0.0) + 
                                          ratios.getOrDefault(EmotionType.GREAT, 0.0);
        
        final double negativeEmotionRatio = ratios.getOrDefault(EmotionType.ANGRY, 0.0) + 
                                          ratios.getOrDefault(EmotionType.DOWN, 0.0);
        
        if (positiveEmotionRatio > 50.0) {
            statistics.append("전반적으로 긍정적인 감정을 많이 느끼셨네요!");
        } else if (negativeEmotionRatio > 30.0) {
            statistics.append("부정적인 감정이 다소 높게 나타났습니다. ");
            if (ratios.getOrDefault(EmotionType.DOWN, 0.0) > 20.0) {
                statistics.append("우울한 감정이 많았던 것 같아요. ");
            }
            if (ratios.getOrDefault(EmotionType.ANGRY, 0.0) > 20.0) {
                statistics.append("화난 감정도 자주 느끼셨네요. ");
            }
            statistics.append("힘든 일이 있었나요?");
        } else {
            statistics.append("감정이 비교적 안정적으로 유지되었습니다.");
        }
        
        return statistics.toString();
    }

    private String getEmotionKoreanName(final EmotionType emotionType) {
        switch (emotionType) {
            case OKAY: return "평범";
            case GOOD: return "좋은";
            case ANGRY: return "화난";
            case DOWN: return "우울한";
            case GREAT: return "최고의";
            default: return "평범";
        }
    }

    private List<StatsResponse.MonthlyChange> getMonthlyChange(final Long bifId, final Integer year, final Integer month) {
        final Integer lastMonth = month == 1 ? 12 : month - 1;
        final Integer lastYear = month == 1 ? year - 1 : year;

        final Optional<Stats> currentStats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        final Optional<Stats> lastStats = statsRepository.findByBifIdAndYearAndMonth(bifId, lastYear, lastMonth);

        final List<StatsResponse.MonthlyChange> monthlyChange = new ArrayList<>();

        try {
            final Map<String, Integer> currentCounts = parseCountsOrEmpty(currentStats);
            final Map<String, Integer> lastCounts = parseCountsOrEmpty(lastStats);

            for (EmotionType emotion : EmotionType.values()) {
                monthlyChange.add(buildMonthlyChangeItem(emotion,
                        currentCounts.getOrDefault(emotion.name(), 0),
                        lastCounts.getOrDefault(emotion.name(), 0)));
            }

        } catch (JsonMappingException e) {
            log.error("JSON mapping error calculating monthly changes for bifId: {}, year: {}, month: {}", bifId, year, month, e);
            addDefaultMonthlyChangeItems(monthlyChange);
        } catch (JsonProcessingException e) {
            log.error("JSON processing error calculating monthly changes for bifId: {}, year: {}, month: {}", bifId, year, month, e);
            addDefaultMonthlyChangeItems(monthlyChange);
        } catch (Exception e) {
            log.error("Unexpected error calculating monthly changes for bifId: {}, year: {}, month: {}", bifId, year, month, e);
            addDefaultMonthlyChangeItems(monthlyChange);
        }

        return monthlyChange;
    }

    private Map<String, Integer> parseCountsOrEmpty(final Optional<Stats> statsOpt) throws JsonProcessingException {
        if (statsOpt.isPresent() && statsOpt.get().getEmotionCounts() != null && !statsOpt.get().getEmotionCounts().trim().isEmpty()) {
            try {
                final String countsJson = unwrapIfQuoted(statsOpt.get().getEmotionCounts());
                log.debug("Parsing emotion counts from stats: {}", countsJson);
                return objectMapper.readValue(countsJson, new TypeReference<Map<String, Integer>>() {});
            } catch (Exception e) {
                log.error("Failed to parse emotion counts from stats: {}. Error: {}", statsOpt.get().getEmotionCounts(), e.getMessage());
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    private StatsResponse.MonthlyChange buildMonthlyChangeItem(final EmotionType emotion,
                                                               final Integer currentValue,
                                                               final Integer previousValue) {
        final Double changePercentage = calculateChangePercentage(previousValue, currentValue);
        return StatsResponse.MonthlyChange.builder()
                .month(LABEL_MONTHLY_CHANGE)
                .emotion(emotion)
                .value(currentValue)
                .previousValue(previousValue)
                .changePercentage(changePercentage)
                .build();
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
        // todo: 실제 사용자 서비스에서 BIF 닉네임 조회 로직 구현 필요
        if (bifId == null) {
            log.warn("BIF ID is null, using default nickname");
            return "BIF";
        }
        
        // 더미 닉네임 생성 (개발/테스트용)
        final String dummyNickname = "BIF_" + bifId;
        log.debug("Generated dummy nickname {} for BIF ID {}", dummyNickname, bifId);
        return dummyNickname;
    }
    
    /**
     * 사용자 서비스 연동을 위한 헬퍼 메소드들
     * todo: 사용자 서비스 완성 후 실제 구현으로 교체
     */
    private List<StatsResponse.KeywordData> createKeywordDataList(final List<Map<String, Object>> topKeywordsData) {
        return topKeywordsData.stream()
                .map(keyword -> StatsResponse.KeywordData.builder()
                        .keyword((String) keyword.get("keyword"))
                        .count((Integer) keyword.get("count"))
                        .rank(topKeywordsData.indexOf(keyword) + 1)
                        .build())
                .toList();
    }
    
    private String generateFallbackGuardianAdvice(final Map<String, Double> ratios) {
        final double positiveRatio = ratios.getOrDefault("GOOD", 0.0) + ratios.getOrDefault("GREAT", 0.0);
        final double negativeRatio = ratios.getOrDefault("ANGRY", 0.0) + ratios.getOrDefault("DOWN", 0.0);
        
        if (positiveRatio > 60.0) {
            return "BIF가 매우 긍정적인 감정을 많이 느끼고 있습니다. 이런 좋은 기분을 유지할 수 있도록 지지해주세요.";
        } else if (negativeRatio > 40.0) {
            return "BIF가 부정적인 감정을 많이 경험하고 있습니다. 따뜻한 관심과 대화를 통해 도움을 주세요.";
        } else {
            return "BIF가 균형잡힌 감정 상태를 유지하고 있습니다. 이런 안정적인 상태를 지지해주세요.";
        }
    }
    
    private Integer getCurrentMonth() {
        return LocalDateTime.now().getMonthValue();
    }
    
    private Integer getCurrentYear() {
        return LocalDateTime.now().getYear();
    }
    
    private StatsResponse buildStatsResponse(final Stats statsData, final Long bifId, final Integer year, final Integer month) {
        try {
            List<StatsResponse.EmotionRatio> emotionRatio = new ArrayList<>();
            
            if (statsData.getEmotionCounts() != null && !statsData.getEmotionCounts().trim().isEmpty()) {
                try {
                    final String countsJson = unwrapIfQuoted(statsData.getEmotionCounts());
                    log.debug("Parsing emotion counts for BIF {}: {}", bifId, countsJson);
                    final Map<String, Integer> emotionCounts = objectMapper.readValue(countsJson, new TypeReference<Map<String, Integer>>() {});
                    
                    emotionRatio = emotionCounts.entrySet().stream()
                            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                            .map(entry -> {
                                try {
                                    return StatsResponse.EmotionRatio.builder()
                                            .emotion(EmotionType.valueOf(entry.getKey().toUpperCase()))
                                            .value(entry.getValue())
                                            .build();
                                } catch (IllegalArgumentException e) {
                                    log.warn("Invalid emotion type '{}' for BIF {}, skipping", entry.getKey(), bifId);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .toList();
                    
                    log.debug("Successfully parsed {} emotion ratios for BIF {}", emotionRatio.size(), bifId);
                } catch (Exception e) {
                    log.error("Failed to parse emotion counts for BIF {}: {}. Raw data: {}", bifId, e.getMessage(), statsData.getEmotionCounts());
                    // JSON 파싱 실패 시 빈 리스트 사용
                    emotionRatio = new ArrayList<>();
                }
            } else {
                log.debug("No emotion counts data for BIF {} in {}년 {}월", bifId, year, month);
            }

            final List<StatsResponse.MonthlyChange> monthlyChange = getMonthlyChange(bifId, year, month);
            final List<Map<String, Object>> topKeywordsData = calculateTopKeywords(bifId, year, month);
            final List<StatsResponse.KeywordData> topKeywords = createKeywordDataList(topKeywordsData);

            return StatsResponse.builder()
                    .statisticsText(statsData.getEmotionStatisticsText())
                    .emotionRatio(emotionRatio)
                    .topKeywords(topKeywords)
                    .monthlyChange(monthlyChange)
                    .build();

        } catch (Exception e) {
            log.error("Error building stats response for bifId: {}, year: {}, month: {}", bifId, year, month, e);
            throw new StatsDataParseException("Failed to build stats response", e);
        }
    }

    private String unwrapIfQuoted(final String jsonLike) {
        if (jsonLike == null || jsonLike.isEmpty()) return jsonLike;
        final String trimmed = jsonLike.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            // remove outer quotes
            String result = trimmed.substring(1, trimmed.length() - 1);
            // unescape backslashes before double quotes
            result = result.replace("\\\"", "\"");
            return result;
        }
        return trimmed;
    }
    
    private String getGuardianAdviceFromStats(final Long bifId) {
        final Integer year = getCurrentYear();
        final Integer month = getCurrentMonth();
        
        final Optional<Stats> stats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        if (stats.isPresent()) {
            return stats.get().getGuardianAdviceText();
        }
    
        return generateGuardianAdviceFromTemplate(calculateEmotionCounts(bifId, year, month));
    }
    
    private String generateGuardianAdviceFromTemplate(final Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return "BIF의 감정 데이터가 없어 조언을 제공할 수 없습니다.";
        }
        
        final Map<String, Double> ratios = emotionCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        entry -> (double) entry.getValue() / total * 100
                ));
        
        final String okayRange = getRangeForPercentage(ratios.getOrDefault("OKAY", 0.0));
        final String goodRange = getRangeForPercentage(ratios.getOrDefault("GOOD", 0.0));
        final String angryRange = getRangeForPercentage(ratios.getOrDefault("ANGRY", 0.0));
        final String downRange = getRangeForPercentage(ratios.getOrDefault("DOWN", 0.0));
        final String greatRange = getRangeForPercentage(ratios.getOrDefault("GREAT", 0.0));
        
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
}
