package com.sage.bif.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.common.util.JsonUtils;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.entity.EmotionAnalysisTemplate;
import com.sage.bif.stats.entity.EmotionType;
import com.sage.bif.stats.entity.Stats;
import com.sage.bif.stats.repository.EmotionAnalysisTemplateRepository;
import com.sage.bif.stats.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final EmotionAnalysisTemplateRepository templateRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public StatsResponse getMonthlyStats(final String username) {
        final Long bifId = getBifIdFromUsername(username);
        final Integer year = getCurrentYear();
        final Integer month = getCurrentMonth();
        
        final Optional<Stats> stats = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        if (stats.isEmpty()) {
            generateMonthlyStats(bifId, year, month);
            return getMonthlyStats(username);
        }

        final Stats statsData = stats.get();
        
        try {
            final Map<String, Integer> emotionCounts = objectMapper.readValue(statsData.getEmotionCounts(), new TypeReference<Map<String, Integer>>() {});
            final List<Map<String, Object>> topKeywordsData = objectMapper.readValue(statsData.getTopKeywords(), new TypeReference<List<Map<String, Object>>>() {});

            final List<StatsResponse.EmotionRatio> emotionRatio = emotionCounts.entrySet().stream()
                    .map(entry -> StatsResponse.EmotionRatio.builder()
                            .emotion(EmotionType.valueOf(entry.getKey().toUpperCase()))
                            .value(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            final List<String> topKeywords = topKeywordsData.stream()
                    .map(keyword -> (String) keyword.get("keyword"))
                    .collect(Collectors.toList());

            final List<StatsResponse.MonthlyChange> monthlyChange = getMonthlyChange(bifId, year, month);

            return StatsResponse.builder()
                    .analysisText(statsData.getEmotionAnalysisText())
                    .emotionRatio(emotionRatio)
                    .topKeywords(topKeywords)
                    .monthlyChange(monthlyChange)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing emotion analysis data for bifId: {}, year: {}, month: {}", bifId, year, month, e);
            throw new RuntimeException("Failed to parse emotion analysis data", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianStatsResponse getGuardianStats(final String username) {
        final Long guardianId = getGuardianIdFromUsername(username);
        final Long bifId = getBifIdFromGuardianId(guardianId);
        final StatsResponse bifStats = getMonthlyStats(username);
        
        final String bifNickname = getBifNickname(bifId);
        final String advice = bifStats.getAnalysisText();
        final String warning = generateWarningMessage(bifStats.getEmotionRatio());

        return GuardianStatsResponse.builder()
                .bifNickname(bifNickname)
                .advice(advice)
                .warning(warning)
                .emotionRatio(bifStats.getEmotionRatio())
                .monthlyChange(bifStats.getMonthlyChange())
                .build();
    }

    @Override
    public void generateMonthlyStats(final Long bifId, final Integer year, final Integer month) {
        final Map<EmotionType, Integer> emotionCounts = calculateEmotionCounts(bifId, year, month);
        final List<Map<String, Object>> topKeywords = calculateTopKeywords(bifId, year, month);
        final String analysisText = generateAnalysisTextFromTemplate(emotionCounts);

        final Stats stats = Stats.builder()
                .bifId(bifId)
                .year(year)
                .month(month)
                .emotionAnalysisText(analysisText)
                .emotionCounts(JsonUtils.toJson(emotionCounts))
                .topKeywords(JsonUtils.toJson(topKeywords))
                .isCurrentMonth(month.equals(getCurrentMonth()) && year.equals(getCurrentYear()))
                .build();

        statsRepository.save(stats);
    }

    private Map<EmotionType, Integer> calculateEmotionCounts(final Long bifId, final Integer year, final Integer month) {
        final Optional<com.sage.bif.stats.entity.Stats> statsOptional = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        final Map<EmotionType, Integer> counts = new HashMap<>();
        for (EmotionType emotion : EmotionType.values()) {
            counts.put(emotion, 0);
        }

        if (statsOptional.isPresent()) {
            final com.sage.bif.stats.entity.Stats stat = statsOptional.get();
            try {
                final Map<String, Integer> emotionCounts = objectMapper.readValue(stat.getEmotionCounts(), new TypeReference<Map<String, Integer>>() {});
                for (Map.Entry<String, Integer> entry : emotionCounts.entrySet()) {
                    final EmotionType emotionType = EmotionType.valueOf(entry.getKey().toUpperCase());
                    counts.put(emotionType, counts.get(emotionType) + entry.getValue());
                }
            } catch (Exception e) {
                log.error("Error parsing emotion counts from stats", e);
            }
        }

        return counts;
    }

    private List<Map<String, Object>> calculateTopKeywords(final Long bifId, final Integer year, final Integer month) {
        final Optional<com.sage.bif.stats.entity.Stats> statsOptional = statsRepository.findByBifIdAndYearAndMonth(bifId, year, month);
        
        final Map<String, Integer> keywordCounts = new HashMap<>();
        
        if (statsOptional.isPresent()) {
            final com.sage.bif.stats.entity.Stats stat = statsOptional.get();
            try {
                final List<Map<String, Object>> keywords = objectMapper.readValue(stat.getTopKeywords(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> keyword : keywords) {
                    final String keywordText = (String) keyword.get("keyword");
                    final Integer count = (Integer) keyword.get("count");
                    keywordCounts.put(keywordText, keywordCounts.getOrDefault(keywordText, 0) + count);
                }
            } catch (Exception e) {
                log.error("Error parsing top keywords from stats", e);
            }
        }

        return keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    final Map<String, Object> keyword = new HashMap<>();
                    keyword.put("keyword", entry.getKey());
                    keyword.put("count", entry.getValue());
                    return keyword;
                })
                .collect(Collectors.toList());
    }

    private String generateAnalysisTextFromTemplate(final Map<EmotionType, Integer> emotionCounts) {
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
        
        // 템플릿 기반 분석 시도
        final String okayRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.OKAY, 0.0));
        final String goodRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.GOOD, 0.0));
        final String angryRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.ANGRY, 0.0));
        final String downRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.DOWN, 0.0));
        final String greatRange = getRangeForPercentage(ratios.getOrDefault(EmotionType.GREAT, 0.0));

        final Optional<EmotionAnalysisTemplate> template = templateRepository.findByEmotionRanges(okayRange, goodRange, angryRange, downRange, greatRange);

        if (template.isPresent()) {
            return template.get().getAnalysisText();
        }

        // 템플릿이 없으면 통계 기반으로 분석 텍스트 생성
        return generateStatisticsBasedAnalysisText(emotionCounts, ratios, dominantEmotion, dominantRatio);
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

    private String generateStatisticsBasedAnalysisText(final Map<EmotionType, Integer> emotionCounts, 
                                                      final Map<EmotionType, Double> ratios, 
                                                      final EmotionType dominantEmotion, 
                                                      final double dominantRatio) {
        final StringBuilder analysis = new StringBuilder();
        
        // 주요 감정 분석
        analysis.append(String.format("이번 달에는 %s한 감정이 %.1f%%로 가장 많이 나타났습니다. ", 
                getEmotionKoreanName(dominantEmotion), dominantRatio));
        
        // 긍정적 감정 비율 계산
        final double positiveEmotionRatio = ratios.getOrDefault(EmotionType.GOOD, 0.0) + 
                                          ratios.getOrDefault(EmotionType.GREAT, 0.0);
        
        // 부정적 감정 비율 계산
        final double negativeEmotionRatio = ratios.getOrDefault(EmotionType.ANGRY, 0.0) + 
                                          ratios.getOrDefault(EmotionType.DOWN, 0.0);
        
        if (positiveEmotionRatio > 50.0) {
            analysis.append("전반적으로 긍정적인 감정을 많이 느끼셨네요!");
        } else if (negativeEmotionRatio > 30.0) {
            analysis.append("부정적인 감정이 다소 높게 나타났습니다. ");
            if (ratios.getOrDefault(EmotionType.DOWN, 0.0) > 20.0) {
                analysis.append("우울한 감정이 많았던 것 같아요. ");
            }
            if (ratios.getOrDefault(EmotionType.ANGRY, 0.0) > 20.0) {
                analysis.append("화난 감정도 자주 느끼셨네요. ");
            }
            analysis.append("힘든 일이 있었나요?");
        } else {
            analysis.append("감정이 비교적 안정적으로 유지되었습니다.");
        }
        
        return analysis.toString();
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

        if (currentStats.isPresent()) {
            try {
                final Map<String, Integer> currentCounts = objectMapper.readValue(currentStats.get().getEmotionCounts(), new TypeReference<Map<String, Integer>>() {});
                final int currentTotal = currentCounts.values().stream().mapToInt(Integer::intValue).sum();

                monthlyChange.add(StatsResponse.MonthlyChange.builder()
                        .month("이번달")
                        .value(currentTotal)
                        .build());
            } catch (Exception e) {
                log.error("Error parsing current month emotion counts", e);
            }
        }

        if (lastStats.isPresent()) {
            try {
                final Map<String, Integer> lastCounts = objectMapper.readValue(lastStats.get().getEmotionCounts(), new TypeReference<Map<String, Integer>>() {});
                final int lastTotal = lastCounts.values().stream().mapToInt(Integer::intValue).sum();

                monthlyChange.add(StatsResponse.MonthlyChange.builder()
                        .month("지난달")
                        .value(lastTotal)
                        .build());
            } catch (Exception e) {
                log.error("Error parsing last month emotion counts", e);
            }
        }

        return monthlyChange;
    }

    private Long getBifIdFromUsername(final String username) {
        // 실제 사용자 서비스에서 BIF ID 조회 로직 구현
        return 1L;
    }
    
    private Long getGuardianIdFromUsername(final String username) {
        // 실제 사용자 서비스에서 보호자 ID 조회 로직 구현
        return 1L;
    }

    private Long getBifIdFromGuardianId(final Long guardianId) {
        // 실제 보호자-BIF 연동 로직 구현
        return 1L;
    }

    private String getBifNickname(final Long bifId) {
    //  실제 사용자 서비스에서 BIF 닉네임 조회 로직 구현
        return "BIF";
    }

    private String generateWarningMessage(final List<StatsResponse.EmotionRatio> emotionRatio) {
        // 지난달과 이번달 감정 비율 비교
        final Integer lastMonth = getCurrentMonth() == 1 ? 12 : getCurrentMonth() - 1;
        final Integer lastYear = getCurrentMonth() == 1 ? getCurrentYear() - 1 : getCurrentYear();
        
        final Optional<Stats> lastMonthStats = statsRepository.findByBifIdAndYearAndMonth(1L, lastYear, lastMonth);
        
        if (lastMonthStats.isEmpty()) {
            return null; // 지난달 데이터가 없으면 경고 메시지 생성하지 않음
        }
        
        try {
            final Map<String, Integer> lastMonthCounts = objectMapper.readValue(lastMonthStats.get().getEmotionCounts(), new TypeReference<Map<String, Integer>>() {});
            final int lastMonthTotal = lastMonthCounts.values().stream().mapToInt(Integer::intValue).sum();
            
            // 이번달 감정 비율 계산
            final int currentTotal = emotionRatio.stream().mapToInt(StatsResponse.EmotionRatio::getValue).sum();
            
            final Map<String, Double> currentRatios = emotionRatio.stream()
                    .collect(Collectors.toMap(
                            ratio -> ratio.getEmotion().name(),
                            ratio -> (double) ratio.getValue() / currentTotal * 100
                    ));
            
            final Map<String, Double> lastMonthRatios = lastMonthCounts.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> (double) entry.getValue() / lastMonthTotal * 100
                    ));
            
            // 우울/불안 감정 증가 체크
            final double currentDownRatio = currentRatios.getOrDefault("DOWN", 0.0);
            final double lastMonthDownRatio = lastMonthRatios.getOrDefault("DOWN", 0.0);
            
            final double currentAngryRatio = currentRatios.getOrDefault("ANGRY", 0.0);
            final double lastMonthAngryRatio = lastMonthRatios.getOrDefault("ANGRY", 0.0);
            
            final double downIncrease = currentDownRatio - lastMonthDownRatio;
            final double angryIncrease = currentAngryRatio - lastMonthAngryRatio;
            
            if (downIncrease > 20.0) {
                return String.format("우울 및 불안 감정 수치가 지난달에 비해 %.1f%% 증가했습니다. 주의가 필요합니다.", downIncrease);
            }
            
            if (angryIncrease > 20.0) {
                return String.format("화난 감정 수치가 지난달에 비해 %.1f%% 증가했습니다. 주의가 필요합니다.", angryIncrease);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error generating warning message", e);
            return null;
        }
    }
    
    private Integer getCurrentMonth() {
        return LocalDateTime.now().getMonthValue();
    }
    
    private Integer getCurrentYear() {
        return LocalDateTime.now().getYear();
    }
}