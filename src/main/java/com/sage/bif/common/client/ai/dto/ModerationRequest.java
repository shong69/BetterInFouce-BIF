package com.sage.bif.common.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Azure OpenAI Moderation API 요청을 위한 DTO
 */
public class ModerationRequest {
    
    @JsonProperty("input")
    private String input;
    
    public ModerationRequest() {}
    
    public ModerationRequest(String input) {
        this.input = input;
    }
    
    public String getInput() {
        return input;
    }
    
    public void setInput(String input) {
        this.input = input;
    }
} 