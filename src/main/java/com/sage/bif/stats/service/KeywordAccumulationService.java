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
    
    public void updateKeywordsWithNewContent(Long bifId, List<String> newKeywords, String diaryContent) {
        try {
            log.info("BIF ID {}의 키워드 누적 업데이트 시작 - 새 키워드: {}, 일기 내용: {}", bifId, newKeywords, diaryContent != null ? diaryContent.substring(0, Math.min(50, diaryContent.length())) : "null");
            
            final LocalDateTime currentYearMonth = getCurrentYearMonth();
            final Optional<Stats> existingStats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            if (existingStats.isPresent()) {
                final Stats stats = existingStats.get();
                final Map<String, Integer> currentKeywords = parseKeywordsFromStats(stats.getTopKeywords());
                
                final Map<String, Integer> updatedKeywords = mergeKeywordsWithValidation(currentKeywords, newKeywords, diaryContent);
                
                final List<String> top5Keywords = extractTop5Keywords(updatedKeywords);
                
                stats.setTopKeywords(objectMapper.writeValueAsString(updatedKeywords));
                statsRepository.save(stats);
                
                log.info("BIF ID {}의 키워드 누적 업데이트 완료. Top5: {}", bifId, top5Keywords);
            } else {
                final Map<String, Integer> initialKeywords = new HashMap<>();
                for (String keyword : newKeywords) {
                    if (keyword != null && !keyword.trim().isEmpty() && isValidKeyword(keyword.trim()) && isKeywordInContent(keyword.trim(), diaryContent)) {
                        initialKeywords.put(keyword.trim(), 1);
                        log.info("새 키워드 추가: {}", keyword.trim());
                    } else {
                        log.warn("키워드 검증 실패 - 키워드: {}, 일기 내용: {}", keyword, diaryContent != null ? diaryContent.substring(0, Math.min(50, diaryContent.length())) : "null");
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
        
        final Set<String> uniqueNewKeywords = new HashSet<>();
        for (String keyword : newKeywords) {
            if (keyword != null && !keyword.trim().isEmpty() && isValidKeyword(keyword.trim())) {
                uniqueNewKeywords.add(keyword.trim());
            }
        }
        
        for (String normalizedKeyword : uniqueNewKeywords) {
            mergedKeywords.merge(normalizedKeyword, 1, Integer::sum);
            log.info("키워드 '{}' 누적: {}회", normalizedKeyword, mergedKeywords.get(normalizedKeyword));
        }
        
        return mergedKeywords;
    }
    
    private Map<String, Integer> mergeKeywordsWithValidation(Map<String, Integer> existingKeywords, List<String> newKeywords, String diaryContent) {
        final Map<String, Integer> mergedKeywords = new HashMap<>(existingKeywords);
        
        final Set<String> uniqueNewKeywords = new HashSet<>();
        for (String keyword : newKeywords) {
            if (keyword != null && !keyword.trim().isEmpty() && isValidKeyword(keyword.trim()) && isKeywordInContent(keyword.trim(), diaryContent)) {
                uniqueNewKeywords.add(keyword.trim());
                log.info("키워드 검증 통과: {}", keyword.trim());
            } else {
                log.warn("키워드 검증 실패 - 키워드: {}, 일기 내용: {}", keyword, diaryContent != null ? diaryContent.substring(0, Math.min(50, diaryContent.length())) : "null");
            }
        }
        
        for (String normalizedKeyword : uniqueNewKeywords) {
            mergedKeywords.merge(normalizedKeyword, 1, Integer::sum);
            log.info("키워드 '{}' 누적: {}회", normalizedKeyword, mergedKeywords.get(normalizedKeyword));
        }
        
        return mergedKeywords;
    }
    
    private boolean isKeywordInContent(String keyword, String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        if (lowerContent.contains(lowerKeyword)) {
            return true;
        }
        
        if (keyword.length() >= 2) {
            String[] words = lowerContent.split("\\s+");
            for (String word : words) {
                if (word.contains(lowerKeyword) || lowerKeyword.contains(word)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean isValidKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        String trimmedKeyword = keyword.trim();
        
        if (trimmedKeyword.length() < 1 || trimmedKeyword.length() > 10) {
            return false;
        }
        
        if (trimmedKeyword.matches("^[0-9\\s\\-_.,!?]+$")) {
            return false;
        }
        
        String[] invalidCategories = {
            "개발", "프로그래밍", "코딩", "기술", "소프트웨어", "하드웨어", "앱", "웹", "모바일",
            "업무", "회의", "프로젝트", "과제", "시험", "학습", "공부", "교육", "훈련",
            "도구", "장비", "기계", "컴퓨터", "스마트폰", "태블릿", "프로그램", "시스템"
        };
        
        if (isPersonName(trimmedKeyword)) {
            return false;
        }
        
        for (String category : invalidCategories) {
            if (trimmedKeyword.contains(category)) {
                return false;
            }
        }
        
        String[] tooGeneric = {
            "일상", "생각", "느낌", "시간", "하루", "오늘", "내일", "어제", "그냥", "정말", "참", "너무"
        };
        
        for (String generic : tooGeneric) {
            if (trimmedKeyword.equals(generic)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isPersonName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = keyword.trim();
        
        if (trimmed.matches("^[가-힣]{2,4}$")) {
            String[] commonSurnames = {
                "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권", "황", "안", "송", "전", "고"
            };
            
            for (String surname : commonSurnames) {
                if (trimmed.startsWith(surname)) {
                    return true;
                }
            }
        }
        
        if (trimmed.matches("^[A-Za-z]{2,20}$")) {
            if (Character.isUpperCase(trimmed.charAt(0)) &&
                trimmed.substring(1).chars().allMatch(Character::isLowerCase)) {
                return true;
            }
        }
 
        String[] commonNames = {
            "민수", "지영", "현우", "서연", "준호", "미영", "성민", "예진", "동현", "수진",
            "John", "Jane", "Mike", "Sarah", "David", "Lisa", "Tom", "Amy", "Chris", "Emma"
        };
        
        for (String name : commonNames) {
            if (trimmed.equalsIgnoreCase(name)) {
                return true;
            }
        }
        
        return false;
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

}
