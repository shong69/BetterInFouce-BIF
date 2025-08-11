package com.sage.bif.common.client.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Azure OpenAI Moderation API 설정
 */
@Data
@Component
@ConfigurationProperties(prefix = "azure.openai.moderation")
public class ModerationConfig {
    
    /**
     * Moderation API 사용 여부
     */
    private boolean enabled = true;
    
    /**
     * 위험도 임계값 (0.0 ~ 1.0)
     * 이 값 이상일 때 위험으로 판단
     */
    private double threshold = 0.7;
    
    /**
     * 위험도 검사 실패 시 차단 여부
     */
    private boolean blockOnFailure = true;
    
    /**
     * 특정 카테고리별 개별 임계값 설정
     */
    private CategoryThresholds categoryThresholds = new CategoryThresholds();
    
    @Data
    public static class CategoryThresholds {
        private double hate = 0.7;
        private double hateThreatening = 0.6;
        private double selfHarm = 0.5;
        private double sexual = 0.7;
        private double sexualMinors = 0.3;
        private double violence = 0.7;
        private double violenceGraphic = 0.6;
    }
}
