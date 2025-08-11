package com.sage.bif.common.client.ai.dto;

/**
 * Azure OpenAI Moderation API 응답을 위한 DTO
 */
public class ModerationResponse {
    
    private boolean flagged;
    private String flaggedCategories;
    
    public ModerationResponse() {}
    
    public ModerationResponse(boolean flagged, String flaggedCategories) {
        this.flagged = flagged;
        this.flaggedCategories = flaggedCategories;
    }
    
    public boolean isFlagged() {
        return flagged;
    }
    
    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }
    
    public String getFlaggedCategories() {
        return flaggedCategories;
    }
    
    public void setFlaggedCategories(String flaggedCategories) {
        this.flaggedCategories = flaggedCategories;
    }
} 