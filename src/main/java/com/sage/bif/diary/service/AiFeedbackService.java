package com.sage.bif.diary.service;

import com.sage.bif.common.client.ai.AiServiceClient;
import com.sage.bif.common.client.ai.AzureContentSafetyClient;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
import com.sage.bif.common.client.ai.dto.ModerationResponse;
import com.sage.bif.common.client.ai.AiSettings;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.diary.entity.AiFeedback;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.diary.model.Emotion;
import com.sage.bif.diary.repository.AiFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final AiFeedbackRepository aiFeedbackRepository;
    private final AiServiceClient aiModelClient;
    private final AzureContentSafetyClient contentSafetyClient;

    public AiFeedback createAiFeedback(Diary diary) {
        AiFeedback feedback = AiFeedback.builder()
            .diary(diary)
            .content("")
            .build();
        
        generateAiFeedbackAll(diary, feedback);
        return feedback;
    }

    public void generateAiFeedbackAll(Diary diary, AiFeedback feedback) {
        
        log.info("AI 피드백 생성 메서드 시작 - 일기 ID: {}", diary.getId());
        
        checkModeration(diary.getContent(), feedback, diary.getUser().getBifId(), diary.getId());
        
        if (feedback.isContentFlagged()) {
            log.info("유해한 콘텐츠로 인해 AI 피드백 생성을 건너뜀 - 일기 ID: {}, 카테고리: {}", 
                diary.getId(), feedback.getContentFlaggedCategories());

            aiFeedbackRepository.save(feedback);
            return;
        }

        String aiFeedbackContent = generateAiFeedback(diary.getContent(), diary.getEmotion(), feedback);

        feedback.setContent(aiFeedbackContent);
        aiFeedbackRepository.save(feedback);
        log.info("AI 피드백 생성 완료 - 일기 ID: {}", diary.getId());
    }

    private String buildFlaggedCategoriesString(ModerationResponse response) {
        StringBuilder categories = new StringBuilder();

        if (response.getCategoriesAnalysis() != null) {
            log.info("=== Azure Content Safety API 응답 분석 시작 ===");
            for (ModerationResponse.CategoryAnalysis category : response.getCategoriesAnalysis()) {
                if (category.getSeverity() != null && category.getSeverity() >= 4) {
                    if (!categories.isEmpty()) {
                        categories.append(", ");
                    }
                    categories.append(category.getCategory().toLowerCase());
                }
            }
            log.info("=== Azure Content Safety API 응답 분석 완료 ===");
        }

        return categories.isEmpty() ? "unknown":categories.toString();
    }


    private static final Map<String, String> CONTENT_FILTER_PATTERNS = Map.of(
        "\"violence\":{\"filtered\":true", "violence(filtered)",
        "\"hate\":{\"filtered\":true", "hate(filtered)", 
        "\"sexual\":{\"filtered\":true", "sexual(filtered)",
        "\"self_harm\":{\"filtered\":true", "self_harm(filtered)",
        "\"jailbreak\":{\"detected\":true", "jailbreak(detected)"
    );

    private void parseOpenAiErrorAndSetFlag(String errorMessage, AiFeedback feedback) {
        try {
            String jsonResponse = extractJsonFromError(errorMessage);
            if (jsonResponse == null || !jsonResponse.contains("content_filter")) {
                feedback.setContentFlagged(true);
                feedback.setContentFlaggedCategories("unknown");
                return;
            }
            
            String categories = extractFilteredCategories(jsonResponse);
            setContentFilterFlag(feedback, categories);
            
        } catch (Exception parseException) {
            log.error("에러 응답 파싱 실패: {}", parseException.getMessage());
        }
    }
    
    private String extractJsonFromError(String errorMessage) {
        if (!errorMessage.contains("{")) {
            return null;
        }
        
        int jsonStart = errorMessage.indexOf("{");
        String jsonResponse = errorMessage.substring(jsonStart);
        log.error("Azure OpenAI 에러 응답 본문: {}", jsonResponse);
        return jsonResponse;
    }
    
    private String extractFilteredCategories(String jsonResponse) {
        return CONTENT_FILTER_PATTERNS.entrySet().stream()
            .filter(entry -> jsonResponse.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.joining(", "));
    }
    
    private void setContentFilterFlag(AiFeedback feedback, String categories) {
        feedback.setContentFlagged(true);
        
        if (!categories.isEmpty()) {
            feedback.setContentFlaggedCategories(categories);
            log.error("필터링된 카테고리: {}", categories);
            log.info("Azure OpenAI 콘텐츠 정책 위반으로 차단된 AI 피드백 - flag 설정 완료. 카테고리: {}", categories);
        } else {
            feedback.setContentFlaggedCategories("openai_content_filter:general");
            log.info("Azure OpenAI 콘텐츠 정책 위반으로 차단된 AI 피드백 - flag 설정 완료");
        }
    }

    public void checkModeration(String content, AiFeedback feedback, Long bifId, Long diaryId) {
        try {
            log.info("콘텐츠 유해성 검사 시작 - BIF ID: {}, 일기 ID: {}", bifId, diaryId);

            ModerationResponse moderationResponse = contentSafetyClient.moderateText(content);

            boolean isFlagged = !moderationResponse.isContentSafe();
            feedback.setContentFlagged(isFlagged);

            if (isFlagged) {
                String flaggedCategories = buildFlaggedCategoriesString(moderationResponse);
                feedback.setContentFlaggedCategories(flaggedCategories);
                log.warn("유해한 콘텐츠 발견 - BIF ID: {}, 일기 ID: {}, 카테고리: {}",
                    bifId, diaryId, flaggedCategories);
            } else {
                feedback.setContentFlaggedCategories(null);
                log.info("콘텐츠가 안전합니다 - BIF ID: {}, 일기 ID: {}", bifId, diaryId);
            }

            log.info("콘텐츠 유해성 검사 완료 - BIF ID: {}, 일기 ID: {}, flagged: {}",
                bifId, diaryId, isFlagged);

        } catch (Exception e) {
            log.error("콘텐츠 유해성 검사 중 오류 발생 - BIF ID: {}, 일기 ID: {}, 오류: {}",
                bifId, diaryId, e.getMessage(), e);

            feedback.setContentFlagged(false);
            feedback.setContentFlaggedCategories(null);
        }
    }

    public String generateAiFeedback(String content, Emotion emotion, AiFeedback feedback) {
        try {
            String userPrompt = content;
            if (emotion != null) {
                userPrompt = String.format("감정: %s%n%n일기 내용:%n%s", 
                    emotion.name(), content);
            }

            AiRequest request = new AiRequest(userPrompt);
            AiResponse response = aiModelClient.generate(request, AiSettings.DIARY_FEEDBACK);
            return response.getContent();
            
        } catch (BaseException e) {
            log.error("AI 피드백 생성 BaseException - 콘텐츠: {}, 에러: {}", content, e.getMessage());
            
            parseOpenAiErrorAndSetFlag(e.getMessage(), feedback);
            
            return null;
        } catch (Exception e) {
            log.error("=== generateAiFeedback 메서드에서 예상치 못한 예외 발생 ===");
            log.error("AI 피드백 생성 예상치 못한 오류 - 콘텐츠: {}, 에러: {}", content, e.getMessage(), e);
            log.error("예외 스택 트레이스:", e);
            return "AI 피드백 생성 중 예상치 못한 오류가 발생했습니다.";
        }
    }

}
