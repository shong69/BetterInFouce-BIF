package com.sage.bif.common.client.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Moderation API 사용 예제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationExample {
    
    private final AzureOpenAiModerationClient moderationClient;
    private final AzureOpenAiClient aiClient;
    
    /**
     * 사용자 입력에 대한 위험도 검사 예제
     */
    public void checkUserInputExample() {
        String userInput = "오늘은 정말 힘든 하루였어요.";
        
        try {
            var result = moderationClient.moderateText(userInput);
            if (result.isFlagged()) {
                log.warn("사용자 입력이 위험합니다: {}", result);
            } else {
                log.info("사용자 입력이 안전합니다.");
            }
        } catch (Exception e) {
            log.error("위험도 검사 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * AI 응답 생성 전후 위험도 검사 예제
     */
    public void aiResponseWithModerationExample() {
        String userPrompt = "오늘 기분이 어때요?";
        
        try {
            // AI 응답 생성 (내부적으로 moderation 체크 수행)
            var aiResponse = aiClient.generate(new com.sage.bif.common.client.ai.dto.AiRequest(userPrompt));
            log.info("AI 응답: {}", aiResponse.getContent());
            
        } catch (com.sage.bif.common.exception.ContentModerationException e) {
            log.error("콘텐츠가 위험도 검사를 통과하지 못했습니다: {}", e.getMessage());
            log.error("위험도 상세: {}", e.getModerationResult());
        } catch (Exception e) {
            log.error("AI 응답 생성 중 오류 발생: {}", e.getMessage());
        }
    }
}
