package com.sage.bif.stats.service;

import com.sage.bif.common.client.ai.AiServiceClient;
import com.sage.bif.common.client.ai.AiSettings;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.stats.entity.EmotionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEmotionAnalysisService {

    private final AiServiceClient aiClient;

    public EmotionAnalysisResult analyzeEmotionFromText(String diaryContent) {
        try {
            log.info("AI 감정 분석 시작: {}", diaryContent.substring(0, Math.min(50, diaryContent.length())));

            double emotionScore = calculateEmotionScore(diaryContent);
            EmotionType dominantEmotion = EmotionType.fromScore(emotionScore);
            List<String> extractedKeywords = extractKeywordsWithAI(diaryContent);

            return EmotionAnalysisResult.builder()
                    .emotionScore(emotionScore)
                    .dominantEmotion(dominantEmotion)
                    .keywords(extractedKeywords)
                    .confidence(0.85)
                    .build();

        } catch (Exception e) {
            log.error("AI 감정 분석 중 오류 발생", e);
            return createDefaultAnalysisResult();
        }
    }

    public String generateStatisticsTextWithAI(Map<EmotionType, Integer> emotionCounts) {
        try {
            String prompt = createStatisticsPrompt(emotionCounts);
            AiRequest request = new AiRequest(prompt);
            String response = aiClient.generate(request, AiSettings.STATS_EMOTION_ANALYSIS).getContent();
            
            log.info("AI 통계 텍스트 생성 완료: {}", response.substring(0, Math.min(100, response.length())));
            return response;
            
        } catch (Exception e) {
            log.error("AI 통계 텍스트 생성 실패", e);
            return generateStatisticsTextFallback(emotionCounts);
        }
    }

    public String generateGuardianAdviceWithAI(Map<EmotionType, Integer> emotionCounts) {
        try {
            String prompt = createGuardianAdvicePrompt(emotionCounts);
            AiRequest request = new AiRequest(prompt);
            String response = aiClient.generate(request, AiSettings.STATS_GUARDIAN_ADVICE).getContent();
            
            log.info("AI 보호자 조언 생성 완료: {}", response.substring(0, Math.min(100, response.length())));
            return response;
            
        } catch (Exception e) {
            log.error("AI 보호자 조언 생성 실패", e);
            return generateGuardianAdviceFallback(emotionCounts);
        }
    }

    private String createStatisticsPrompt(Map<EmotionType, Integer> emotionCounts) {
        int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return "이번 달에는 작성된 일기가 없습니다. 첫 번째 일기를 작성해보세요!";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 사용자의 월별 감정 통계입니다. ");
        prompt.append("감정별 개수와 비율을 바탕으로 간단하고 격려가 되는 요약을 작성해주세요.%n%n");
        
        appendEmotionStatistics(prompt, emotionCounts, total);
        
        prompt.append("%n요구사항:%n");
        prompt.append("1. 감정 상태를 정확하게 분석하여 요약%n");
        prompt.append("2. 2-3문장으로 작성 (100-150자)%n");
        prompt.append("3. 한국어로 작성%n");
        prompt.append("4. 따뜻하고 격려가 되는 톤으로 작성%n");
        prompt.append("5. 예시 일기문이나 구체적인 제안은 하지 말 것%n");
        prompt.append("6. 감정일기 피드백처럼 공감적이고 건설적인 내용으로 작성%n");
        prompt.append("7. 월 초반(1-3일)에는 '이번 달의 시작'을 언급하고, 일기 작성 습관을 격려하는 내용 포함%n");
        prompt.append("8. 구체적인 예시나 질문은 절대 제외하고, 일반적이고 격려적인 메시지로 작성%n");
        prompt.append("9. '예시를 써드릴게요', '도와드릴게요', '어떨까요?' 같은 표현 금지");

        return prompt.toString();
    }

    private String createGuardianAdvicePrompt(Map<EmotionType, Integer> emotionCounts) {
        int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return "사용자의 감정 데이터가 없어 조언을 제공할 수 없습니다.";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 사용자의 월별 감정 통계입니다. ");
        prompt.append("보호자 입장에서 간단한 조언을 제공해주세요.%n%n");
        
        appendEmotionStatistics(prompt, emotionCounts, total);
        
        prompt.append("%n요구사항:%n");
        prompt.append("1. 사용자의 감정 상태를 정확하게 분석%n");
        prompt.append("2. 2-3문장으로 작성 (100-150자)%n");
        prompt.append("3. 한국어로 작성%n");
        prompt.append("4. 보호자로서의 따뜻한 마음가짐으로 작성%n");
        prompt.append("5. 구체적인 행동 지시나 예시는 하지 말 것%n");
        prompt.append("6. 공감적이고 실용적인 조언 제공%n");
        prompt.append("7. 구체적인 예시나 질문은 절대 제외하고, 일반적이고 격려적인 메시지로 작성%n");
        prompt.append("8. '예시를 써드릴게요', '도와드릴게요', '어떨까요?' 같은 표현 금지");

        return prompt.toString();
    }

    private void appendEmotionStatistics(StringBuilder prompt, Map<EmotionType, Integer> emotionCounts, int total) {
        prompt.append("감정 통계:%n");
        for (Map.Entry<EmotionType, Integer> entry : emotionCounts.entrySet()) {
            if (entry.getValue() > 0) {
                double percentage = (double) entry.getValue() / total * 100;
                prompt.append(String.format("- %s: %d회 (%.1f%%)%n", 
                    entry.getKey().getKoreanName(), entry.getValue(), percentage));
            }
        }
    }

    private List<String> extractKeywordsWithAI(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String prompt = String.format(
                "다음 감정일기 내용에서 개인의 감정과 경험을 나타내는 의미있는 키워드만 추출해주세요.\n\n" +
                "일기 내용에 직접적으로 언급되지 않은 단어는 추출하지 마세요.\n\n" +
                "일기 내용: %s\n\n" +
                "추출 기준:\n" +
                "1. 개인의 감정 상태 (기쁨, 슬픔, 우울감, 만족감, 스트레스, 안도감, 걱정, 설렘 등)\n" +
                "2. 개인적 경험과 관련된 구체적 대상 (가족, 친구, 애완동물, 취미, 음식, 장소 등)\n" +
                "3. 개인의 일상 활동 (운동, 독서, 요리, 산책, 영화감상, 음악감상 등)\n" +
                "4. 개인적 상황과 환경 (생일, 기념일, 휴가, 병원, 학교, 집 등)\n\n" +
                "제외 기준:\n" +
                "- 개인정보 관련 (사람 이름, 실명, 별명, 닉네임 등)\n" +
                "- 기술/개발 관련 용어 (깃허브, 코드, 프로그래밍, 개발, 프로젝트, 버그 등)\n" +
                "- 업무/학업 관련 용어 (회의, 업무, 과제, 시험, 발표, 보고서 등)\n" +
                "- 일반적/추상적 단어 (오늘, 하루, 시간, 생각, 느낌, 일상, 그냥, 정말 등)\n" +
                "- 도구/장비 관련 용어 (컴퓨터, 스마트폰, 앱, 프로그램 등)\n\n" +
                "쉼표로 구분하여 3-5개만 추출하세요.\n\n" +
                "예시:\n" +
                "- '오늘 협회 회의실 사용이 불가하다고 전달 받아서 서울역에서 모이기로 했었는데... 회의하러 갈 생각에 참 우울하네' → 협회, 회의실, 서울역, 우울감\n" +
                "- '친구와 카페에서 커피를 마셨다. 정말 기분이 좋았다' → 친구, 카페, 커피, 기쁨\n" +
                "- '오늘 뱃지 게이미피케이션 관련된거 완성된 것 같아서 너무 좋다. 지금 시간은 03시 50분 깃허브에 올리고 드디어 잘 수 있겠어' → 만족감, 완성감, 안도감",
                content.substring(0, Math.min(500, content.length()))
            );

            AiRequest request = new AiRequest(prompt);
            String response = aiClient.generate(request, AiSettings.STATS_KEYWORD_EXTRACTION).getContent();
            
            log.info("AI 키워드 추출 원본 응답: {}", response);
            
            String[] keywords = response.split(",");
            List<String> extractedKeywords = new java.util.ArrayList<>();
            String lowerContent = content.toLowerCase();
            
            for (String keyword : keywords) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty() && trimmed.length() <= 10) {
                    boolean isValid = false;
                    
                    if (lowerContent.contains(trimmed.toLowerCase())) {
                        isValid = true;
                    }
                    
                    if (!isValid && trimmed.length() >= 2) {
                        String[] words = lowerContent.split("\\s+");
                        for (String word : words) {
                            if (word.contains(trimmed.toLowerCase()) || trimmed.toLowerCase().contains(word)) {
                                isValid = true;
                                break;
                            }
                        }
                    }
                    
                    if (isValid) {
                        extractedKeywords.add(trimmed);
                        log.info("키워드 검증 통과: {} (일기 내용: {})", trimmed, content.substring(0, Math.min(100, content.length())));
                    } else {
                        log.warn("키워드 검증 실패 - 일기 내용에 없음: {} (일기 내용: {})", trimmed, content.substring(0, Math.min(100, content.length())));
                    }
                }
            }
            
            if (extractedKeywords.isEmpty()) {
                log.info("AI 키워드 추출 결과가 비어있음 - fallback 사용");
                return extractKeywordsFallback(content);
            }
            
            return extractedKeywords.subList(0, Math.min(5, extractedKeywords.size()));
            
        } catch (Exception e) {
            log.error("AI 키워드 추출 실패", e);
            return extractKeywordsFallback(content);
        }
    }


    private List<String> extractKeywordsFallback(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] commonKeywords = {
                "가족", "친구", "직장", "학교", "공부", "일", "취미", "운동", "음식",
                "여행", "영화", "음악", "책", "게임", "쇼핑", "요리", "정리", "청소"
        };

        List<String> foundKeywords = new java.util.ArrayList<>();
        String lowerContent = content.toLowerCase();

        for (String keyword : commonKeywords) {
            if (lowerContent.contains(keyword)) {
                foundKeywords.add(keyword);
            }
        }

        if (foundKeywords.isEmpty()) {
            return new ArrayList<>();
        }

        return foundKeywords.subList(0, Math.min(5, foundKeywords.size()));
    }

    private String generateStatisticsTextFallback(Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        if (total == 0) {
            return "이번 달에는 작성된 일기가 없습니다. 첫 번째 일기를 작성해보세요!";
        }

        final int currentDay = java.time.LocalDate.now().getDayOfMonth();
        final boolean isEarlyMonth = currentDay <= 3;
        
        if (isEarlyMonth && total == 1) {
            return "새로운 한 달의 시작을 축하드려요! 첫 번째 일기를 작성하셨네요. 이번 달에도 꾸준히 일기를 써나가며 마음의 변화를 기록해보세요. 작은 습관이 큰 변화를 만들어낼 거예요.";
        }

        final EmotionType dominantEmotion = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(EmotionType.OKAY);

        final double dominantRatio = (double) emotionCounts.get(dominantEmotion) / total * 100;

        if (dominantEmotion.isPositive()) {
            return String.format("이번 달에는 %s한 감정이 %.1f%%로 가장 많이 나타났습니다. 긍정적인 감정을 많이 느끼셨네요! 이런 좋은 기분을 유지할 수 있도록 응원합니다.", 
                    dominantEmotion.getKoreanName(), dominantRatio);
        } else if (dominantEmotion.isNegative()) {
            return String.format("이번 달에는 %s한 감정이 %.1f%%로 가장 많이 나타났습니다. 힘든 일이 있었나요? 내일은 더 나아질 거예요. 함께 힘내봐요!", 
                    dominantEmotion.getKoreanName(), dominantRatio);
        } else {
            return String.format("이번 달에는 %s한 감정이 %.1f%%로 가장 많이 나타났습니다. 감정이 비교적 안정적으로 유지되었네요. 이런 균형잡힌 상태를 지지합니다.", 
                    dominantEmotion.getKoreanName(), dominantRatio);
        }
    }

    private String generateGuardianAdviceFallback(Map<EmotionType, Integer> emotionCounts) {
        final int total = emotionCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        if (total == 0) {
            return "사용자의 감정 데이터가 없어 조언을 제공할 수 없습니다.";
        }

        final int currentDay = java.time.LocalDate.now().getDayOfMonth();
        final boolean isEarlyMonth = currentDay <= 3;
        
        if (isEarlyMonth && total == 1) {
            return "새로운 한 달의 시작을 축하드려요! 첫 번째 일기를 작성하셨네요. 이번 달에도 꾸준히 일기를 써나가며 마음의 변화를 기록해보세요. 작은 습관이 큰 변화를 만들어낼 거예요.";
        }

        final double positiveRatio = (emotionCounts.get(EmotionType.GOOD) + emotionCounts.get(EmotionType.GREAT)) / (double) total * 100;
        final double negativeRatio = (emotionCounts.get(EmotionType.ANGRY) + emotionCounts.get(EmotionType.DOWN)) / (double) total * 100;

        if (positiveRatio > 60.0) {
            return "긍정적인 감정을 많이 느끼고 있습니다. 지지해주세요.";
        } else if (negativeRatio > 40.0) {
            return "부정적인 감정을 많이 경험하고 있습니다. 따뜻한 관심을 가져주세요.";
        } else {
            return "균형잡힌 감정 상태를 유지하고 있습니다. 지지해주세요.";
        }
    }


    private double calculateEmotionScore(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        String lowerContent = content.toLowerCase();

        Map<String, Double> positiveWords = Map.of(
                "행복", 2.0, "기쁨", 2.0, "즐거움", 1.5, "좋음", 1.0, "만족", 1.5,
                "감사", 1.5, "사랑", 2.0, "희망", 1.5, "성공", 1.0, "축하", 1.5
        );

        Map<String, Double> negativeWords = Map.of(
                "슬픔", -2.0, "화남", -2.0, "짜증", -1.5, "불만", -1.0, "걱정", -1.5,
                "스트레스", -1.5, "실패", -1.0, "우울", -2.0, "불안", -1.5, "지루함", -0.5
        );

        Map<String, Double> neutralWords = Map.of(
                "보통", 0.0, "평범", 0.0, "일반", 0.0, "그냥", 0.0
        );

        for (Map.Entry<String, Double> entry : positiveWords.entrySet()) {
            if (lowerContent.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }

        for (Map.Entry<String, Double> entry : negativeWords.entrySet()) {
            if (lowerContent.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }

        for (Map.Entry<String, Double> entry : neutralWords.entrySet()) {
            if (lowerContent.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }

        return Math.max(-2.0, Math.min(2.0, score));
    }


    private EmotionAnalysisResult createDefaultAnalysisResult() {
        return EmotionAnalysisResult.builder()
                .emotionScore(0.0)
                .dominantEmotion(EmotionType.OKAY)
                .keywords(new ArrayList<>())
                .confidence(0.5)
                .build();
    }

    @lombok.Getter
    public static class EmotionAnalysisResult {
        private double emotionScore;
        private EmotionType dominantEmotion;
        private List<String> keywords;
        private double confidence;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final EmotionAnalysisResult result = new EmotionAnalysisResult();

            public Builder emotionScore(double emotionScore) {
                result.emotionScore = emotionScore;
                return this;
            }

            public Builder dominantEmotion(EmotionType dominantEmotion) {
                result.dominantEmotion = dominantEmotion;
                return this;
            }

            public Builder keywords(List<String> keywords) {
                result.keywords = keywords;
                return this;
            }

            public Builder confidence(double confidence) {
                result.confidence = confidence;
                return this;
            }

            public EmotionAnalysisResult build() {
                return result;
            }
        }

    }

}
