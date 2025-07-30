package com.sage.bif.common.client.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * AI 채팅 설정 객체
 * 모든 AI 기능에서 공통으로 사용하는 설정을 관리
 */
@Data
@Builder
public class AiChatSettings {
    
    /**
     * 시스템 프롬프트
     */
    private String systemPrompt;
    
    /**
     * 창의성 조절 (0.0 ~ 2.0)
     * 0.0: 일관성 있는 응답
     * 1.0: 균형잡힌 응답
     * 2.0: 매우 창의적인 응답
     */
    private double temperature;
    
    /**
     * 최대 토큰 수
     */
    private int maxTokens;
    
    /**
     * 기본 설정
     */
    public static AiChatSettings getDefault() {
        return AiChatSettings.builder()
            .systemPrompt("당신은 도움이 되는 AI 어시스턴트입니다.")
            .temperature(0.7)
            .maxTokens(1000)
            .build();
    }
} 