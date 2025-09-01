package com.sage.bif.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.stats.entity.Stats;
import com.sage.bif.stats.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KeywordAccumulationService {

    private final StatsRepository statsRepository;
    private final ObjectMapper objectMapper;

    public void updateKeywordsWithNewContent(Long bifId, List<String> newKeywords) {
        try {
            log.info("BIF ID {}의 키워드 누적 업데이트 시작 - 새 키워드: {}", bifId, newKeywords);
            
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            if (existingStats.isPresent()) {
                final Stats stats = existingStats.get();
                final Map<String, Integer> currentKeywords = parseKeywordsFromStats(stats.getTopKeywords());
                
                // 새 키워드와 기존 키워드 통합
                final Map<String, Integer> updatedKeywords = mergeKeywords(currentKeywords, newKeywords);
                
                // Top5 키워드 추출 및 정렬
                final List<String> top5Keywords = extractTop5Keywords(updatedKeywords);
                
                // 통계 업데이트 - 키워드 맵을 JSON으로 저장
                stats.setTopKeywords(objectMapper.writeValueAsString(updatedKeywords));
                statsRepository.save(stats);
                
                log.info("BIF ID {}의 키워드 누적 업데이트 완료. Top5: {}", bifId, top5Keywords);
            } else {
                // 통계가 없으면 새로 생성
                final Map<String, Integer> initialKeywords = new HashMap<>();
                for (String keyword : newKeywords) {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        initialKeywords.put(keyword.trim(), 1);
                    }
                }
                
                final Stats newStats = Stats.builder()
                        .bifId(bifId)
                        .yearMonth(currentYearMonth)
                        .topKeywords(objectMapper.writeValueAsString(initialKeywords))
                        .build();
                
                statsRepository.save(newStats);
                log.info("BIF ID {}의 새로운 통계 및 키워드 생성 완료", bifId);
            }
            
        } catch (Exception e) {
            log.error("키워드 누적 업데이트 중 오류 발생 - bifId: {}", bifId, e);
        }
    }



    public Map<String, Integer> getKeywordFrequency(Long bifId, LocalDateTime yearMonth) {
        try {
            final Optional<Stats> stats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, yearMonth);
            
            if (stats.isPresent() && stats.get().getTopKeywords() != null) {
                final Map<String, Integer> keywordCounts = parseKeywordsFromStats(stats.get().getTopKeywords());
                log.info("BIF ID {}의 누적 키워드 빈도수: {}", bifId, keywordCounts);
                return keywordCounts;
            }
            
            log.info("BIF ID {}의 누적 키워드가 없음", bifId);
            return new HashMap<>();
            
        } catch (Exception e) {
            log.error("키워드 빈도수 조회 중 오류 발생 - bifId: {}", bifId, e);
            return new HashMap<>();
        }
    }

    private Map<String, Integer> mergeKeywords(Map<String, Integer> existingKeywords, List<String> newKeywords) {
        final Map<String, Integer> mergedKeywords = new HashMap<>(existingKeywords);
        
        for (String keyword : newKeywords) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                final String normalizedKeyword = keyword.trim();
                mergedKeywords.merge(normalizedKeyword, 1, Integer::sum);
            }
        }
        
        return mergedKeywords;
    }

    private List<String> extractTop5Keywords(Map<String, Integer> keywordCounts) {
        return keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<String, Integer> parseKeywordsFromStats(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            // 기존 키워드가 맵 형태인 경우 (권장)
            if (keywordsJson.startsWith("{")) {
                return objectMapper.readValue(keywordsJson, new TypeReference<>() {});
            }
            
            // 기존 키워드가 단순 리스트 형태인 경우 (이전 버전 호환성)
            if (keywordsJson.startsWith("[")) {
                List<String> keywords = objectMapper.readValue(keywordsJson, new TypeReference<>() {});
                Map<String, Integer> keywordCounts = new HashMap<>();
                for (String keyword : keywords) {
                    keywordCounts.put(keyword, 1);
                }
                return keywordCounts;
            }
            
            return new HashMap<>();
            
        } catch (Exception e) {
            log.error("키워드 JSON 파싱 실패: {}", keywordsJson, e);
            return new HashMap<>();
        }
    }

    private LocalDateTime getCurrentYearMonth() {
        final LocalDateTime now = LocalDateTime.now();
        return LocalDateTime.of(now.getYear(), now.getMonth(), 1, 0, 0, 0);
    }




}
