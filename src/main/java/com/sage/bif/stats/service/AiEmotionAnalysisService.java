package com.sage.bif.stats.service;

import com.sage.bif.common.client.ai.AzureOpenAiClient;
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

    private final AzureOpenAiClient aiClient;

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
            String response = aiClient.generate(request).getContent();
            
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
            String response = aiClient.generate(request).getContent();
            
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
        prompt.append("다음은 BIF 사용자의 월별 감정 통계입니다. ");
        prompt.append("보호자 입장에서 간단한 조언을 제공해주세요.%n%n");
        
        appendEmotionStatistics(prompt, emotionCounts, total);
        
        prompt.append("%n요구사항:%n");
        prompt.append("1. BIF의 감정 상태를 정확하게 분석%n");
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
            String prompt = String.format("""
                다음 일기 내용에서 의미있는 핵심 키워드 5개를 추출해주세요:
                
                %s
                
                요구사항:
                1. 일기 내용의 주요 주제나 감정을 나타내는 단어
                2. 명사 위주로 추출
                3. 쉼표로 구분하여 5개만 반환
                4. 한국어로 작성
                5. 설명이나 추가 텍스트 없이 키워드만 반환""",
                content.substring(0, Math.min(500, content.length()))
            );

            AiRequest request = new AiRequest(prompt);
            String response = aiClient.generate(request).getContent();
            
            // AI 응답에서 키워드 추출
            String[] keywords = response.split(",");
            List<String> extractedKeywords = new java.util.ArrayList<>();
            
            for (String keyword : keywords) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty() && trimmed.length() <= 10) {
                    extractedKeywords.add(trimmed);
                }
            }
            
            // 빈 키워드일 때 기본값 제거
            if (extractedKeywords.isEmpty()) {
                return new ArrayList<>();
            }
            
            return extractedKeywords.subList(0, Math.min(5, extractedKeywords.size()));
            
        } catch (Exception e) {
            log.error("AI 키워드 추출 실패", e);
            return extractKeywordsFallback(content);
        }
    }

    // 캐릭터 메시지 생성 메서드 제거

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

        // 월 초반(1-3일)인지 확인
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

        // 월 초반(1-3일)인지 확인
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

    // 캐릭터 관련 메서드 제거

    private EmotionAnalysisResult createDefaultAnalysisResult() {
        return EmotionAnalysisResult.builder()
                .emotionScore(0.0)
                .dominantEmotion(EmotionType.OKAY)
                .keywords(new ArrayList<>()) // 기본 키워드 제거
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

            // 캐릭터 관련 메서드 제거

            public Builder confidence(double confidence) {
                result.confidence = confidence;
                return this;
            }

            public EmotionAnalysisResult build() {
                return result;
            }
        }

        // Lombok @Getter로 대체됨
    }
}
