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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StatsServiceImpl implements StatsService, ApplicationContextAware {

    private final StatsRepository statsRepository;
    private final EmotionStatisticsTemplateRepository templateRepository;
    private final GuardianAdviceTemplateRepository guardianAdviceTemplateRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public StatsResponse getMonthlyStats(final String username) {
        final Long bifId = getBifIdFromUsername(username);
        final Integer year = getCurrentYear();
        final Integer month = getCurrentMonth();
        
        final Optional<Stats> stats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        if (stats.isEmpty()) {
            // 트랜잭션 메서드를 직접 호출하지 않고 별도 메서드로 분리
            applicationContext.getBean(StatsServiceImpl.class).generateMonthlyStats(bifId, year, month);
            return applicationContext.getBean(StatsServiceImpl.class).getMonthlyStats(username);
        }

        return buildStatsResponse(stats.get(), bifId, year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianStatsResponse getGuardianStats(final String username) {
        final Long guardianId = getGuardianIdFromUsername(username);
        final Long bifId = getBifIdFromGuardianId(guardianId);
        
        // Guardian은 연결된 BIF의 username이 아닌 BIF ID로 통계 조회
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
    
    /**
     * BIF ID로 직접 월별 통계 조회 (보호자용)
     */
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
        // 전월 데이터를 기반으로 감정 통계 텍스트와 보호자 조언 생성
        final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, year, month);
        final String statisticsText = generateStatisticsTextFromTemplate(emotionCounts);
        final String guardianAdvice = generateGuardianAdviceFromTemplate(emotionCounts);

        final Stats stats = Stats.builder()
                .bifId(bifId)
                .year(year)
                .month(month)
                .emotionStatisticsText(statisticsText)
                .guardianAdviceText(guardianAdvice)
                .emotionCounts(null)  // 일기 작성 전까지는 NULL
                .topKeywords(null)    // 일기 작성 전까지는 NULL
                .build();

        statsRepository.save(stats);
    }
    
    @Override
    public void generateMonthlyEmotionStatistics(final Long bifId, final Integer year, final Integer month) {
        // 기존 통계 데이터가 있는지 확인
        final Optional<Stats> existingStats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        if (existingStats.isPresent()) {
            // 기존 통계 데이터가 있으면 감정 통계 텍스트와 보호자 조언만 업데이트
            final Stats stats = existingStats.get();
            final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, year, month);
            final String statisticsText = generateStatisticsTextFromTemplate(emotionCounts);
            final String guardianAdvice = generateGuardianAdviceFromTemplate(emotionCounts);
            
            stats.setEmotionStatisticsText(statisticsText);
            stats.setGuardianAdviceText(guardianAdvice);
            statsRepository.save(stats);
            
            log.info("BIF ID {}의 {}년 {}월 감정 통계 텍스트와 보호자 조언 업데이트 완료", bifId, year, month);
        } else {
            // 기존 통계 데이터가 없으면 전체 통계 생성
            log.warn("BIF ID {}의 {}년 {}월 통계 데이터가 없어 전체 통계를 생성합니다.", bifId, year, month);
            applicationContext.getBean(StatsServiceImpl.class).generateMonthlyStats(bifId, year, month);
        }
    }

    private Map<EmotionType, Integer> calculateEmotionCounts(final Long bifId, final Integer year, final Integer month) {
        final Optional<com.sage.bif.stats.entity.Stats> statsOptional = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        final Map<EmotionType, Integer> counts = new EnumMap<>(EmotionType.class);
        for (EmotionType emotion : EmotionType.values()) {
            counts.put(emotion, 0);
        }

        if (statsOptional.isPresent()) {
            final com.sage.bif.stats.entity.Stats stat = statsOptional.get();
            try {
                if (stat.getEmotionCounts() != null) {
                    final Map<String, Integer> emotionCounts = objectMapper.readValue(stat.getEmotionCounts(), new TypeReference<Map<String, Integer>>() {});
                    for (Map.Entry<String, Integer> entry : emotionCounts.entrySet()) {
                        final EmotionType emotionType = EmotionType.valueOf(entry.getKey().toUpperCase());
                        counts.put(emotionType, counts.get(emotionType) + entry.getValue());
                    }
                } else {
                    log.debug("No emotion counts data available for BIF ID {} in {}년 {}월", bifId, year, month);
                }
            } catch (Exception e) {
                log.error("Error parsing emotion counts from stats", e);
            }
        }

        return counts;
    }

    private List<Map<String, Object>> calculateTopKeywords(final Long bifId, final Integer year, final Integer month) {
        final Optional<com.sage.bif.stats.entity.Stats> statsOptional = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        if (statsOptional.isPresent()) {
            final com.sage.bif.stats.entity.Stats stat = statsOptional.get();
            try {
                if (stat.getTopKeywords() != null) {
                    final List<Map<String, Object>> keywords = objectMapper.readValue(stat.getTopKeywords(), new TypeReference<List<Map<String, Object>>>() {});
                    log.debug("Retrieved {} keywords from stats for BIF ID {}: {}", keywords.size(), bifId, keywords);
                    return keywords;
                } else {
                    log.debug("No top keywords data available for BIF ID {} in {}년 {}월", bifId, year, month);
                    return new ArrayList<>();
                }
            } catch (Exception e) {
                log.error("Error parsing top keywords from stats for BIF ID {}: {}", bifId, e.getMessage());
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

        // 요구사항: 감정 분석은 감정 통계를 토대로 이루어진다
        final Map<EmotionType, Double> ratios = emotionCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (double) entry.getValue() / total * 100
                ));

        // 감정 비율을 기반으로 분석 텍스트 생성
        final EmotionType dominantEmotion = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(EmotionType.OKAY);

        final double dominantRatio = ratios.get(dominantEmotion);
        
        // 템플릿 기반 통계 시도 (emotion_statistics_template 테이블 활용)
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

        // 템플릿이 없으면 통계 기반으로 통계 텍스트 생성
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
        
        // 주요 감정 통계
        statistics.append(String.format("이번 달에는 %s한 감정이 %.1f%%로 가장 많이 나타났습니다. ",
                getEmotionKoreanName(dominantEmotion), dominantRatio));
        
        // 긍정적 감정 비율 계산
        final double positiveEmotionRatio = ratios.getOrDefault(EmotionType.GOOD, 0.0) +
                                          ratios.getOrDefault(EmotionType.GREAT, 0.0);
        
        // 부정적 감정 비율 계산
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
            // 현재 달과 지난달 감정 데이터 파싱
            final Map<String, Integer> currentCounts = currentStats.isPresent() && currentStats.get().getEmotionCounts() != null ?
                    objectMapper.readValue(currentStats.get().getEmotionCounts(), new TypeReference<Map<String, Integer>>() {}) :
                    new HashMap<>();
            
            final Map<String, Integer> lastCounts = lastStats.isPresent() && lastStats.get().getEmotionCounts() != null ?
                    objectMapper.readValue(lastStats.get().getEmotionCounts(), new TypeReference<Map<String, Integer>>() {}) :
                    new HashMap<>();

            // 각 감정별로 월별 변화 계산
            for (EmotionType emotion : EmotionType.values()) {
                final String emotionKey = emotion.name();
                final Integer currentValue = currentCounts.getOrDefault(emotionKey, 0);
                final Integer previousValue = lastCounts.getOrDefault(emotionKey, 0);
                
                // 변화율 계산 (지난달 기준)
                final Double changePercentage = calculateChangePercentage(previousValue, currentValue);

                monthlyChange.add(StatsResponse.MonthlyChange.builder()
                        .month("감정별 변화")
                        .emotion(emotion)
                        .value(currentValue)
                        .previousValue(previousValue)
                        .changePercentage(changePercentage)
                        .build());
            }

        } catch (Exception e) {
            log.error("Error calculating monthly emotion changes for bifId: {}, year: {}, month: {}", bifId, year, month, e);
            
            // 오류 발생 시 빈 리스트 반환하는 대신 기본 데이터라도 제공
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

        return monthlyChange;
    }

    /**
     * 변화율 계산 메소드
     */
    private Double calculateChangePercentage(final Integer previousValue, final Integer currentValue) {
        if (previousValue == 0) {
            return currentValue > 0 ? 100.0 : 0.0; // 지난달이 0이고 이번달이 있으면 100% 증가
        }
        return ((double) (currentValue - previousValue) / previousValue) * 100.0;
    }

    private Long getBifIdFromUsername(final String username) {
        // todo: 실제 사용자 서비스 완성 후 구현 필요
        // 현재는 username을 기반으로 더미 ID 생성 (개발/테스트용)
        if (username == null || username.trim().isEmpty()) {
            log.warn("Username is null or empty, using default BIF ID");
            return 1L;
        }
        
        // username 해시를 이용한 더미 ID 생성 (일관성 보장)
        final Long dummyId = (long) (username.hashCode() & 0x7fffffff) % 1000 + 1;
        log.debug("Generated dummy BIF ID {} for username: {}", dummyId, username);
        return dummyId;
    }
    
    private Long getGuardianIdFromUsername(final String username) {
        // todo: 실제 사용자 서비스 완성 후 구현 필요
        if (username == null || username.trim().isEmpty()) {
            log.warn("Username is null or empty, using default Guardian ID");
            return 1L;
        }
        
        // Guardian은 BIF ID와 다른 범위의 ID 사용 (2000~2999)
        final Long dummyId = (long) (username.hashCode() & 0x7fffffff) % 1000 + 2000;
        log.debug("Generated dummy Guardian ID {} for username: {}", dummyId, username);
        return dummyId;
    }

    private Long getBifIdFromGuardianId(final Long guardianId) {
        // todo: 실제 보호자-BIF 관계 테이블에서 조회 로직 구현 필요
        if (guardianId == null) {
            log.warn("Guardian ID is null, using default BIF ID");
            return 1L;
        }
        
        // 더미 로직: Guardian ID에서 BIF ID 매핑 (개발/테스트용)
        // 실제로는 guardian_bif_relationship 테이블에서 조회해야 함
        final Long dummyBifId = (guardianId - 2000) % 1000 + 1;
        log.debug("Mapped Guardian ID {} to dummy BIF ID {}", guardianId, dummyBifId);
        return dummyBifId;
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
    private boolean isValidUsername(final String username) {
        return username != null && !username.trim().isEmpty() && username.length() >= 3;
    }
    
    @SuppressWarnings("unused")
    private boolean isUserExists(final String username) {
        // todo: 실제 사용자 존재 여부 확인 로직 구현
        // 사용자 서비스 완성 후 활용 예정
        return isValidUsername(username); // 임시로 유효성만 체크
    }
    
    /**
     * 키워드 데이터 리스트 생성 헬퍼 메소드
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
    


    

    
    /**
     * 기본 보호자 조언 생성 (템플릿이 없을 때 사용)
     */
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
    
    /**
     * Stats 엔티티를 StatsResponse로 변환하는 공통 메서드
     */
    private StatsResponse buildStatsResponse(final Stats statsData, final Long bifId, final Integer year, final Integer month) {
        try {
            // 감정 통계가 NULL인 경우 처리
            List<StatsResponse.EmotionRatio> emotionRatio = new ArrayList<>();
            
            if (statsData.getEmotionCounts() != null) {
                final Map<String, Integer> emotionCounts = objectMapper.readValue(statsData.getEmotionCounts(), new TypeReference<Map<String, Integer>>() {});
                emotionRatio = emotionCounts.entrySet().stream()
                        .map(entry -> StatsResponse.EmotionRatio.builder()
                                .emotion(EmotionType.valueOf(entry.getKey().toUpperCase()))
                                .value(entry.getValue())
                                .build())
                        .toList();
            }

            final List<StatsResponse.MonthlyChange> monthlyChange = getMonthlyChange(bifId, year, month);

            // calculateTopKeywords 메소드를 사용하여 키워드 데이터 처리
            final List<Map<String, Object>> topKeywordsData = calculateTopKeywords(bifId, year, month);
            final List<StatsResponse.KeywordData> topKeywords = createKeywordDataList(topKeywordsData);

            return StatsResponse.builder()
                    .statisticsText(statsData.getEmotionStatisticsText())
                    .emotionRatio(emotionRatio)
                    .topKeywords(topKeywords)
                    .monthlyChange(monthlyChange)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing emotion statistics data for bifId: {}, year: {}, month: {}", bifId, year, month, e);
            throw new StatsDataParseException("Failed to parse emotion statistics data", e);
        }
    }
    
    /**
     * 보호자 조언을 Stats 테이블에서 가져오기
     */
    private String getGuardianAdviceFromStats(final Long bifId) {
        final Integer year = getCurrentYear();
        final Integer month = getCurrentMonth();
        
        final Optional<Stats> stats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        if (stats.isPresent()) {
            return stats.get().getGuardianAdviceText();
        }
        
        // 저장된 조언이 없으면 실시간으로 생성
        return generateGuardianAdviceFromTemplate(calculateEmotionCounts(bifId, year, month));
    }
    
    /**
     * 보호자 조언 템플릿에서 조언 생성 (generateGuardianAdvice와 동일한 로직)
     */
    private String generateGuardianAdviceFromTemplate(final Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return "BIF의 감정 데이터가 없어 조언을 제공할 수 없습니다.";
        }
        
        // 감정 비율 계산
        final Map<String, Double> ratios = emotionCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        entry -> (double) entry.getValue() / total * 100
                ));
        
        // 템플릿 기반 조언 생성 (guardian_advice_template 테이블 활용)
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
        
        // 템플릿이 없으면 기본 조언 생성
        return generateFallbackGuardianAdvice(ratios);
    }
}
