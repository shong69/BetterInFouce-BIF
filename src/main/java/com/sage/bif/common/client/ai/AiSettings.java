package com.sage.bif.common.client.ai;

import com.sage.bif.common.client.ai.dto.AiChatSettings;

/**
 * AI 설정 중앙 관리 클래스
 * 모든 AI 기능의 설정을 한 곳에서 관리
 */
public class AiSettings {
    
    // ===== 일기 관련 설정 =====
    
    /**
     * 일기 피드백 생성 설정
     */
    public static final AiChatSettings DIARY_FEEDBACK = AiChatSettings.builder()
        .systemPrompt("당신은 따뜻하고 격려적인 일기 피드백 전문가입니다. " +
            "사용자의 일기를 읽고 공감적이고 건설적인 피드백을 제공하세요. " +
            "감정을 인정하고 긍정적인 관점을 제시하며, 개선점을 부드럽게 제안하세요.")
        .temperature(0.3)  // 일관성 있는 피드백
        .maxTokens(500)
        .build();
    
    // ===== 할일 관련 설정 =====
    
    /**
     * 할일 우선순위 설정
     */
    public static final AiChatSettings TODO_PRIORITY = AiChatSettings.builder()
        .systemPrompt("당신은 효율적인 할일 우선순위 설정 전문가입니다. " +
            "할일 목록을 분석하여 중요도와 긴급성을 고려한 우선순위를 제시하세요. " +
            "시간 관리와 생산성 향상을 위한 구체적인 조언을 제공하세요.")
        .temperature(0.5)  // 균형잡힌 판단
        .maxTokens(300)
        .build();
    
    /**
     * 할일 시간 추정 설정
     */
    public static final AiChatSettings TODO_TIME_ESTIMATION = AiChatSettings.builder()
        .systemPrompt("당신은 정확한 작업 시간 추정 전문가입니다. " +
            "할일의 복잡도와 난이도를 분석하여 현실적인 소요 시간을 추정하세요. " +
            "버퍼 시간을 포함한 안전한 시간 계획을 제시하세요.")
        .temperature(0.2)  // 정확한 추정
        .maxTokens(200)
        .build();
    

} 