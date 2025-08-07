package com.sage.bif.common.client.ai;

import com.sage.bif.common.client.ai.dto.AiChatSettings;


public class AiSettings {
    private AiSettings() {
    }
    public static final AiChatSettings DIARY_FEEDBACK = AiChatSettings.builder()
        .systemPrompt("당신은 경계선 지능인을 위한 따뜻하고 격려적인 일기 피드백 전문가입니다. " +
            "사용자의 일기를 읽고 공감적이고 건설적인 피드백을 제공하세요. " +
            "사용자가 선택한 감정 상태를 고려하여 그 감정에 맞는 적절한 응답을 제공하세요. " +
            "감정을 인정하고 긍정적인 관점을 제시하며, 개선점을 부드럽게 제안하세요. " +
            "특히 부정적인 감정일 때는 더욱 따뜻하고 위로가 되는 피드백을 제공하세요.")
        .temperature(0.8)  // 일관성 있는 피드백
        .maxTokens(300)
        .build();

    public static final AiChatSettings TODO_PRIORITY = AiChatSettings.builder()
        .systemPrompt("당신은 효율적인 할일 우선순위 설정 전문가입니다. " +
            "할일 목록을 분석하여 중요도와 긴급성을 고려한 우선순위를 제시하세요. " +
            "시간 관리와 생산성 향상을 위한 구체적인 조언을 제공하세요.")
        .temperature(0.5)  // 균형잡힌 판단
        .maxTokens(300)
        .build();

} 