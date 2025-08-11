package com.sage.bif.diary.service;

import com.sage.bif.common.client.ai.AiServiceClient;
import com.sage.bif.common.client.ai.AzureOpenAiModerationClient;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
import com.sage.bif.common.client.ai.dto.ModerationResponse;
import com.sage.bif.common.client.ai.AiSettings;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.diary.entity.AiFeedback;
import com.sage.bif.diary.entity.Diary;
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
    private final AzureOpenAiModerationClient moderationClient;
    
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
                String aiFeedbackContent = generateAiFeedback(diary.getContent());
                
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
        try {
            ModerationResponse moderationResponse = moderationClient.moderate(content);
            if (moderationResponse.isFlagged()) {
                feedback.setContentFlagged(true);
                feedback.setContentFlaggedCategories(moderationResponse.getFlaggedCategories());
                log.warn("부적절한 콘텐츠 감지 - 사용자: {}, 일기: {}, 카테고리: {}", 
                    bifId, diaryId, moderationResponse.getFlaggedCategories());
            } else {
                feedback.setContentFlagged(false);
                feedback.setContentFlaggedCategories(null);
            }
        } catch (Exception e) {
            log.warn("Moderation 체크 실패: {}", e.getMessage());
            feedback.setContentFlagged(false);
            feedback.setContentFlaggedCategories(null);
        }
    }
    
    public String generateAiFeedback(String content) {
        try {
            log.info("AI 피드백 생성 시작");
            
            AiRequest request = new AiRequest(content);
            AiResponse response = aiModelClient.generate(request, AiSettings.DIARY_FEEDBACK);
            
            log.info("AI 피드백 생성 완료");
            return response.getContent();
            
        } catch (BaseException e) {
            log.error("AI 피드백 생성 BaseException: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("AI 피드백 생성 예상치 못한 오류: {}", e.getMessage());
            return null;
        }
    }
} 