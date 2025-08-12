package com.sage.bif.common.client.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatSettings {

    private String systemPrompt;

    private double temperature;

    private int maxTokens;

    public static AiChatSettings getDefault() {
        return AiChatSettings.builder()
                .systemPrompt("당신은 도움이 되는 AI 어시스턴트입니다.")
                .temperature(0.7)
                .maxTokens(1000)
                .build();
    }

}
