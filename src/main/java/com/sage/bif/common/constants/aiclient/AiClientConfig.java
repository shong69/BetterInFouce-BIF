package com.sage.bif.common.constants.aiclient;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "spring.ai.azure.openai")
@Getter
@Setter
public class AiClientConfig {
    
    private String endpoint;
    private String apiKey;
    private Deployment deployment = new Deployment();
    
    @Getter
    @Setter
    public static class Deployment {
        private String name = "gpt-35-turbo";
    }
    
    // AI 클라이언트 설정
} 