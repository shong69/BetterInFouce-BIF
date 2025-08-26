package com.sage.bif.stats.service;

import com.sage.bif.stats.entity.EmotionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEmotionAnalysisService {

    public EmotionAnalysisResult analyzeEmotionFromText(String diaryContent) {
        try {
            log.info("AI 감정 분석 시작: {}", diaryContent.substring(0, Math.min(50, diaryContent.length())));

            double emotionScore = calculateEmotionScore(diaryContent);
            EmotionType dominantEmotion = EmotionType.fromScore(emotionScore);
            List<String> extractedKeywords = extractKeywords(diaryContent);
            String characterName = determineCharacter(emotionScore);
            String characterMessage = generateCharacterMessage(characterName, emotionScore, extractedKeywords);

            return EmotionAnalysisResult.builder()
                    .emotionScore(emotionScore)
                    .dominantEmotion(dominantEmotion)
                    .keywords(extractedKeywords)
                    .characterName(characterName)
                    .characterMessage(characterMessage)
                    .confidence(0.85)
                    .build();

        } catch (Exception e) {
            log.error("AI 감정 분석 중 오류 발생", e);
            return createDefaultAnalysisResult();
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
                "보통", 0.0, "평범", 0.0, "일반", 0.0, "그냥", 0.0, "보통", 0.0
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

    private List<String> extractKeywords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return List.of("일상", "생활", "하루");
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
            foundKeywords.add("일상");
        }

        return foundKeywords.subList(0, Math.min(5, foundKeywords.size()));
    }

    private String determineCharacter(double emotionScore) {
        if (emotionScore >= 1.5) return "행복한 토끼";
        if (emotionScore >= 0.5) return "즐거운 강아지";
        if (emotionScore >= -0.5) return "현명한 거북이";
        if (emotionScore >= -1.5) return "걱정 많은 고양이";
        return "화난 호랑이";
    }

    private String generateCharacterMessage(String characterName, double emotionScore, List<String> keywords) {
        if (emotionScore >= 1.5) {
            return String.format("오늘 정말 좋은 하루였네요! %s에 대한 이야기가 들려주셔서 기뻐요. 이런 좋은 기분을 계속 유지해보세요! 🎉", 
                    keywords.isEmpty() ? "일상" : keywords.get(0));
        } else if (emotionScore >= 0.5) {
            return String.format("오늘 꽤 좋은 하루였네요. %s에 대한 이야기가 인상적이에요. 조금 더 긍정적인 마음가짐으로 하루를 보내보세요! 😊", 
                    keywords.isEmpty() ? "일상" : keywords.get(0));
        } else if (emotionScore >= -0.5) {
            return String.format("오늘은 보통의 하루였네요. %s에 대한 이야기가 있어서 다행이에요. 내일은 더 좋은 하루가 될 거예요! 🐢", 
                    keywords.isEmpty() ? "일상" : keywords.get(0));
        } else if (emotionScore >= -1.5) {
            return String.format("오늘 조금 힘든 하루였나요? %s에 대한 이야기가 있어서 걱정이 되네요. 내일은 더 나아질 거예요! 🐱", 
                    keywords.isEmpty() ? "일상" : keywords.get(0));
        } else {
            return String.format("오늘 정말 힘든 하루였나요? %s에 대한 이야기가 있어서 마음이 아파요. 힘내세요! 내일은 분명 좋아질 거예요! 🐯", 
                    keywords.isEmpty() ? "일상" : keywords.get(0));
        }
    }

    private EmotionAnalysisResult createDefaultAnalysisResult() {
        return EmotionAnalysisResult.builder()
                .emotionScore(0.0)
                .dominantEmotion(EmotionType.OKAY)
                .keywords(List.of("일상"))
                .characterName("현명한 거북이")
                .characterMessage("오늘 하루도 수고하셨어요. 내일은 더 좋은 하루가 될 거예요! 🐢")
                .confidence(0.5)
                .build();
    }

    public static class EmotionAnalysisResult {
        private double emotionScore;
        private EmotionType dominantEmotion;
        private List<String> keywords;
        private String characterName;
        private String characterMessage;
        private double confidence;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private EmotionAnalysisResult result = new EmotionAnalysisResult();

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

            public Builder characterName(String characterName) {
                result.characterName = characterName;
                return this;
            }

            public Builder characterMessage(String characterMessage) {
                result.characterMessage = characterMessage;
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

        public double getEmotionScore() { return emotionScore; }
        public EmotionType getDominantEmotion() { return dominantEmotion; }
        public List<String> getKeywords() { return keywords; }
        public String getCharacterName() { return characterName; }
        public String getCharacterMessage() { return characterMessage; }
        public double getConfidence() { return confidence; }
    }
}
