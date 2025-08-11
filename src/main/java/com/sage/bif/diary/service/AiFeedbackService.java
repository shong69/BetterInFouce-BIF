package com.sage.bif.diary.service;

import com.sage.bif.common.client.ai.AiServiceClient;
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

    }
    
    public String generateAiFeedback(String content, Emotion emotion) {
        log.info("=== generateAiFeedback 메서드 시작 ===");
        log.info("입력 파라미터 - content: {}, emotion: {}", content, emotion);
        
        try {
            log.info("AI 피드백 생성 시작 - 감정: {}, 콘텐츠: {}", emotion, content);
            
            if (aiModelClient == null) {
                log.error("aiModelClient가 null입니다!");
                throw new RuntimeException("aiModelClient가 null입니다");
            }
            
            String userPrompt = content;
            if (emotion != null) {
                userPrompt = String.format("감정: %s%n%n일기 내용:%n%s", 
                    emotion.name(), content);
            }
            
            log.info("AI 모델에 전송할 프롬프트: {}", userPrompt);
            
            AiRequest request = new AiRequest(userPrompt);
            log.info("AiRequest 생성 완료: {}", request);
            
            log.info("aiModelClient.generate() 호출 시작");
            AiResponse response = aiModelClient.generate(request, AiSettings.DIARY_FEEDBACK);
            log.info("AI 응답 수신 완료: {}", response);
            
            log.info("AI 피드백 생성 완료: {}", response.getContent());
            log.info("=== generateAiFeedback 메서드 정상 완료 ===");
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
