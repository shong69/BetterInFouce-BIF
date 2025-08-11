package com.sage.bif.common.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModerationRequest {
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("input")
    private String input;
    
    public ModerationRequest() {}
    
    public ModerationRequest(String input) {
        this.model = "text-moderation-latest"; 
        this.input = input;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getInput() {
        return input;
    }
    
    public void setInput(String input) {
        this.input = input;
    }
}
