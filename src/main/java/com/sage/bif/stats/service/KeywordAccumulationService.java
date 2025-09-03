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
                
                // 새 키워드만 추가 (중복 방지)
                final Map<String, Integer> updatedKeywords = mergeKeywords(currentKeywords, newKeywords);
                
                final List<String> top5Keywords = extractTop5Keywords(updatedKeywords);
                
                stats.setTopKeywords(objectMapper.writeValueAsString(updatedKeywords));
                statsRepository.save(stats);
                
                log.info("BIF ID {}의 키워드 누적 업데이트 완료. Top5: {}", bifId, top5Keywords);
            } else {
                final Map<String, Integer> initialKeywords = new HashMap<>();
                for (String keyword : newKeywords) {
                    if (keyword != null && !keyword.trim().isEmpty() && isValidKeyword(keyword.trim())) {
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
    
    public void initializeKeywords(Long bifId, Map<String, Integer> initialKeywords) {
        try {
            log.info("BIF ID {}의 키워드 초기화 시작 - 초기 키워드: {}", bifId, initialKeywords);
            
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            if (existingStats.isPresent()) {
                final Stats stats = existingStats.get();
                stats.setTopKeywords(objectMapper.writeValueAsString(initialKeywords));
                statsRepository.save(stats);
                log.info("BIF ID {}의 키워드 초기화 완료", bifId);
            } else {
                final Stats newStats = Stats.builder()
                        .bifId(bifId)
                        .yearMonth(currentYearMonth)
                        .topKeywords(objectMapper.writeValueAsString(initialKeywords))
                        .build();
                
                statsRepository.save(newStats);
                log.info("BIF ID {}의 새로운 통계 및 키워드 초기화 완료", bifId);
            }
            
        } catch (Exception e) {
            log.error("키워드 초기화 중 오류 발생 - bifId: {}", bifId, e);
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
        
        // 새 키워드들을 Set으로 중복 제거 (같은 일기 내 중복 방지)
        final Set<String> uniqueNewKeywords = new HashSet<>();
        for (String keyword : newKeywords) {
            if (keyword != null && !keyword.trim().isEmpty() && isValidKeyword(keyword.trim())) {
                uniqueNewKeywords.add(keyword.trim());
            }
        }
        
        // 중복 제거된 새 키워드들을 기존 키워드에 누적
        for (String normalizedKeyword : uniqueNewKeywords) {
            mergedKeywords.merge(normalizedKeyword, 1, Integer::sum);
            log.info("키워드 '{}' 누적: {}회", normalizedKeyword, mergedKeywords.get(normalizedKeyword));
        }
        
        return mergedKeywords;
    }
    
    private boolean isValidKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        // 잘못된 키워드 패턴들
        String[] invalidPatterns = {
            "사용불가", "서울역", "우울감", "협회", "회의실", "일상", "일반", "보통", "평범",
            "그냥", "그저", "그런", "이런", "저런", "어떤", "무엇", "언제", "어디", "왜", "어떻게"
        };
        
        for (String pattern : invalidPatterns) {
            if (keyword.contains(pattern)) {
                return false;
            }
        }
        
        // 키워드 길이 검증 (1-10자)
        if (keyword.length() < 1 || keyword.length() > 10) {
            return false;
        }
        
        // 특수문자나 숫자만으로 구성된 키워드 제외
        if (keyword.matches("^[0-9\\s\\-_.,!?]+$")) {
            return false;
        }
        
        return true;
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
            if (keywordsJson.startsWith("{")) {
                return objectMapper.readValue(keywordsJson, new TypeReference<>() {});
            }
            
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
