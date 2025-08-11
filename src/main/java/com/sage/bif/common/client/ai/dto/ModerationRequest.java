package com.sage.bif.common.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
