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

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final AiFeedbackRepository aiFeedbackRepository;
    private final AiServiceClient aiModelClient;
    private final AzureContentSafetyClient contentSafetyClient;

    @Transactional
    public void regenerateAiFeedbackIfNeeded(Diary diary, AiFeedback feedback) {
        boolean needsRegeneration = false;

        if (feedback.getContent() == null){
            needsRegeneration = true;
        }

        if (!feedback.isContentFlagged() && feedback.getContentFlaggedCategories() == null) {
            needsRegeneration = true;
        }

        if (needsRegeneration) {
            log.info("AI 피드백 재생성 시작 - 일기 ID: {}", diary.getId());
            try {
                String aiFeedbackContent = generateAiFeedback(diary.getContent(), diary.getEmotion());

                feedback.setContent(aiFeedbackContent);
                checkModeration(diary.getContent(), feedback, diary.getUser().getBifId(), diary.getId());
                aiFeedbackRepository.save(feedback);

                log.info("AI 피드백 재생성 완료 - 일기 ID: {}", diary.getId());

            } catch (Exception e) {
                log.error("AI 피드백 재생성 실패 - 일기 ID: {}, 오류: {}", diary.getId(), e.getMessage());
            }
        }
    }

    public void checkModeration(String content, AiFeedback feedback, Long bifId, Long diaryId) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("검사할 콘텐츠가 비어있습니다. BIF ID: {}, 일기 ID: {}", bifId, diaryId);
            feedback.setContentFlagged(false);
            feedback.setContentFlaggedCategories(null);
            return;
        }

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

    private String buildFlaggedCategoriesString(ModerationResponse response) {
        StringBuilder categories = new StringBuilder();

        if (response.getCategoriesAnalysis() != null) {
            for (ModerationResponse.CategoryAnalysis category : response.getCategoriesAnalysis()) {
                if (category.getSeverity() != null && category.getSeverity() > 0) {
                    if (categories.length() > 0) {
                        categories.append(", ");
                    }
                    categories.append(category.getCategory())
                             .append("(")
                             .append(getSeverityDescription(category.getSeverity()))
                             .append(")");
                }
            }
        }

        return categories.length() > 0 ? categories.toString() : "Unknown";
    }


    private String getSeverityDescription(Integer severity) {
        if (severity == null) return "Unknown";

        switch (severity) {
            case 1: return "Low";
            case 2: return "Medium";
            case 3: return "High";
            case 4: return "Very High";
            default: return "Unknown";
        }
    }
    
    public String generateAiFeedback(String content, Emotion emotion) {
        try {
            if (aiModelClient == null) {
                log.error("aiModelClient가 null입니다!");
                throw new RuntimeException("aiModelClient가 null입니다");
            }

            String userPrompt = content;
            if (emotion != null) {
                userPrompt = String.format("감정: %s%n%n일기 내용:%n%s", 
                    emotion.name(), content);
            }

            AiRequest request = new AiRequest(userPrompt);
            AiResponse response = aiModelClient.generate(request, AiSettings.DIARY_FEEDBACK);
            return response.getContent();
            
        } catch (BaseException e) {
            log.error("=== generateAiFeedback 메서드에서 BaseException 발생 ===");
            log.error("AI 피드백 생성 BaseException - 콘텐츠: {}, 에러: {}", content, e.getMessage(), e);
            log.error("BaseException 스택 트레이스:", e);
            return null;
        } catch (Exception e) {
            log.error("=== generateAiFeedback 메서드에서 예상치 못한 예외 발생 ===");
            log.error("AI 피드백 생성 예상치 못한 오류 - 콘텐츠: {}, 에러: {}", content, e.getMessage(), e);
            log.error("예외 스택 트레이스:", e);
            return null;
        }
    }

}
