package com.sage.bif.common.client.ai;

import com.sage.bif.common.client.ai.dto.AiChatSettings;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;

/**
 * AI 모델 클라이언트 인터페이스
 * 다양한 AI 서비스와의 통신을 추상화
 */
public interface AiServiceClient {
    
    /**
     * 기본 설정으로 AI 응답 생성
     * 
     * @param request 사용자 요청
     * @return AI 응답
     */
    AiResponse generate(AiRequest request);
    
    /**
     * 커스텀 설정으로 AI 응답 생성
     * 
     * @param request 사용자 요청
     * @param settings AI 채팅 설정
     * @return AI 응답
     */
    AiResponse generate(AiRequest request, AiChatSettings settings);
} 