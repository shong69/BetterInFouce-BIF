package com.sage.bif.common.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModerationRequest {
    
    @JsonProperty("text")
    private String text;
    
    public ModerationRequest() {}
    
    public ModerationRequest(String text) {
        this.text = text;
    }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
}
