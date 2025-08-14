package com.sage.bif.common.client.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

import java.util.List;
@NoArgsConstructor
public class ModerationResponse {
    
    @JsonProperty("categoriesAnalysis")
    private List<CategoryAnalysis> categoriesAnalysis;
    
    public static class CategoryAnalysis {
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("severity")
        private Integer severity;
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public Integer getSeverity() { return severity; }
        public void setSeverity(Integer severity) { this.severity = severity; }
    }
    
    public List<CategoryAnalysis> getCategoriesAnalysis() { return categoriesAnalysis; }
    public void setCategoriesAnalysis(List<CategoryAnalysis> categoriesAnalysis) { this.categoriesAnalysis = categoriesAnalysis; }
    
    public boolean hasHighSeverityContent() {
        if (categoriesAnalysis == null) return false;
        return categoriesAnalysis.stream()
            .anyMatch(cat -> cat.getSeverity() != null && cat.getSeverity() >= 3);
    }
    
    public boolean isContentSafe() {
        return !hasHighSeverityContent();
    }
    
}
