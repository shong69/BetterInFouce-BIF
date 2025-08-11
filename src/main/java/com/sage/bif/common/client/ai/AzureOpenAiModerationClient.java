package com.sage.bif.common.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureOpenAiModerationClient {
    
    @Value("${spring.ai.azure.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.azure.openai.endpoint}")
    private String endpoint;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ModerationConfig moderationConfig;
    
    /**
     * 입력 텍스트의 위험도를 검사합니다.
     * @param inputText 검사할 텍스트
     * @return ModerationResult 위험도 검사 결과
     */
    public ModerationResult moderateText(String inputText) {
        // Moderation API가 비활성화된 경우 기본적으로 통과
        if (!moderationConfig.isEnabled()) {
            log.debug("Content moderation is disabled, skipping check for: {}", inputText);
            return createPassedModerationResult();
        }
        try {
            String moderationEndpoint = String.format("%s/openai/deployments/text-moderation-001/input?api-version=2025-01-01-preview", 
                endpoint);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("input", inputText);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(moderationEndpoint, entity, JsonNode.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE, 
                    "Moderation API HTTP 상태 코드: " + response.getStatusCode());
            }
            
            JsonNode body = response.getBody();
            if (body == null) {
                throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID, 
                    "Moderation API 응답 본문이 비어있습니다.");
            }
            
            return parseModerationResponse(body);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE, 
                    "Moderation API 키 인증 실패: " + e.getMessage());
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new BaseException(ErrorCode.COMMON_AI_QUOTA_EXCEEDED, 
                    "Moderation API 호출 한도 초과: " + e.getMessage());
            } else {
                throw new BaseException(ErrorCode.COMMON_AI_REQUEST_FAILED, 
                    "Moderation API HTTP 클라이언트 오류: " + e.getMessage());
            }
        } catch (HttpServerErrorException e) {
            throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE, 
                "Moderation API 서비스 내부 오류: " + e.getMessage());
        } catch (Exception e) {
            throw new BaseException(ErrorCode.COMMON_AI_MODEL_ERROR, 
                "Moderation API 예상치 못한 오류: " + e.getMessage());
        }
    }
    
    private ModerationResult parseModerationResponse(JsonNode body) {
        try {
            if (!body.has("results") || !body.get("results").isArray() || body.get("results").isEmpty()) {
                throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID, 
                    "Moderation API 응답에 results가 없습니다.");
            }
            
            JsonNode result = body.get("results").get(0);
            
            if (!result.has("flagged")) {
                throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID, 
                    "Moderation API 응답에 flagged 필드가 없습니다.");
            }
            
            boolean isFlagged = result.get("flagged").asBoolean();
            
            ModerationResult moderationResult = new ModerationResult();
            moderationResult.setFlagged(isFlagged);
            
            if (isFlagged && result.has("categories")) {
                JsonNode categories = result.get("categories");
                moderationResult.setHate(categories.has("hate") ? categories.get("hate").asBoolean() : false);
                moderationResult.setHateThreatening(categories.has("hate/threatening") ? categories.get("hate/threatening").asBoolean() : false);
                moderationResult.setSelfHarm(categories.has("self-harm") ? categories.get("self-harm").asBoolean() : false);
                moderationResult.setSexual(categories.has("sexual") ? categories.get("sexual").asBoolean() : false);
                moderationResult.setSexualMinors(categories.has("sexual/minors") ? categories.get("sexual/minors").asBoolean() : false);
                moderationResult.setViolence(categories.has("violence") ? categories.get("violence").asBoolean() : false);
                moderationResult.setViolenceGraphic(categories.has("violence/graphic") ? categories.get("violence/graphic").asBoolean() : false);
            }
            
            // 설정 기반 위험도 재판단
            if (result.has("category_scores")) {
                JsonNode categoryScores = result.get("category_scores");
                moderationResult.setHateScore(categoryScores.has("hate") ? categoryScores.get("hate").asDouble() : 0.0);
                moderationResult.setHateThreateningScore(categoryScores.has("hate/threatening") ? categoryScores.get("hate/threatening").asDouble() : 0.0);
                moderationResult.setSelfHarmScore(categoryScores.has("self-harm") ? categoryScores.get("self-harm").asDouble() : 0.0);
                moderationResult.setSexualScore(categoryScores.has("sexual") ? categoryScores.get("sexual").asDouble() : 0.0);
                moderationResult.setSexualMinorsScore(categoryScores.has("sexual/minors") ? categoryScores.get("sexual/minors").asDouble() : 0.0);
                moderationResult.setViolenceScore(categoryScores.has("violence") ? categoryScores.get("violence").asDouble() : 0.0);
                moderationResult.setViolenceGraphicScore(categoryScores.has("violence/graphic") ? categoryScores.get("violence/graphic").asDouble() : 0.0);
                
                // 설정 기반 위험도 판단
                moderationResult.setFlagged(isContentFlagged(moderationResult));
            }
            
            return moderationResult;
            
        } catch (Exception e) {
            throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID, 
                "Moderation API 응답 파싱 실패: " + e.getMessage());
        }
    }
    
    /**
     * 위험도 검사를 통과한 결과를 생성합니다.
     * @return ModerationResult
     */
    private ModerationResult createPassedModerationResult() {
        ModerationResult result = new ModerationResult();
        result.setFlagged(false);
        result.setHate(false);
        result.setHateThreatening(false);
        result.setSelfHarm(false);
        result.setSexual(false);
        result.setSexualMinors(false);
        result.setViolence(false);
        result.setViolenceGraphic(false);
        
        result.setHateScore(0.0);
        result.setHateThreateningScore(0.0);
        result.setSelfHarmScore(0.0);
        result.setSexualScore(0.0);
        result.setSexualMinorsScore(0.0);
        result.setViolenceScore(0.0);
        result.setViolenceGraphicScore(0.0);
        
        return result;
    }
    
    /**
     * 설정 기반으로 콘텐츠가 위험한지 판단합니다.
     * @param result ModerationResult
     * @return boolean 위험 여부
     */
    private boolean isContentFlagged(ModerationResult result) {
        ModerationConfig.CategoryThresholds thresholds = moderationConfig.getCategoryThresholds();
        
        return result.getHateScore() >= thresholds.getHate() ||
               result.getHateThreateningScore() >= thresholds.getHateThreatening() ||
               result.getSelfHarmScore() >= thresholds.getSelfHarm() ||
               result.getSexualScore() >= thresholds.getSexual() ||
               result.getSexualMinorsScore() >= thresholds.getSexualMinors() ||
               result.getViolenceScore() >= thresholds.getViolence() ||
               result.getViolenceGraphicScore() >= thresholds.getViolenceGraphic();
    }
    
    /**
     * Moderation API 검사 결과를 담는 내부 클래스
     */
    public static class ModerationResult {
        private boolean flagged;
        private boolean hate;
        private boolean hateThreatening;
        private boolean selfHarm;
        private boolean sexual;
        private boolean sexualMinors;
        private boolean violence;
        private boolean violenceGraphic;
        
        private double hateScore;
        private double hateThreateningScore;
        private double selfHarmScore;
        private double sexualScore;
        private double sexualMinorsScore;
        private double violenceScore;
        private double violenceGraphicScore;
        
        // Getters and Setters
        public boolean isFlagged() { return flagged; }
        public void setFlagged(boolean flagged) { this.flagged = flagged; }
        
        public boolean isHate() { return hate; }
        public void setHate(boolean hate) { this.hate = hate; }
        
        public boolean isHateThreatening() { return hateThreatening; }
        public void setHateThreatening(boolean hateThreatening) { this.hateThreatening = hateThreatening; }
        
        public boolean isSelfHarm() { return selfHarm; }
        public void setSelfHarm(boolean selfHarm) { this.selfHarm = selfHarm; }
        
        public boolean isSexual() { return sexual; }
        public void setSexual(boolean sexual) { this.sexual = sexual; }
        
        public boolean isSexualMinors() { return sexualMinors; }
        public void setSexualMinors(boolean sexualMinors) { this.sexualMinors = sexualMinors; }
        
        public boolean isViolence() { return violence; }
        public void setViolence(boolean violence) { this.violence = violence; }
        
        public boolean isViolenceGraphic() { return violenceGraphic; }
        public void setViolenceGraphic(boolean violenceGraphic) { this.violenceGraphic = violenceGraphic; }
        
        public double getHateScore() { return hateScore; }
        public void setHateScore(double hateScore) { this.hateScore = hateScore; }
        
        public double getHateThreateningScore() { return hateThreateningScore; }
        public void setHateThreateningScore(double hateThreateningScore) { this.hateThreateningScore = hateThreateningScore; }
        
        public double getSelfHarmScore() { return selfHarmScore; }
        public void setSelfHarmScore(double selfHarmScore) { this.selfHarmScore = selfHarmScore; }
        
        public double getSexualScore() { return sexualScore; }
        public void setSexualScore(double sexualScore) { this.sexualScore = sexualScore; }
        
        public double getSexualMinorsScore() { return sexualMinorsScore; }
        public void setSexualMinorsScore(double sexualMinorsScore) { this.sexualMinorsScore = sexualMinorsScore; }
        
        public double getViolenceScore() { return violenceScore; }
        public void setViolenceScore(double violenceScore) { this.violenceScore = violenceScore; }
        
        public double getViolenceGraphicScore() { return violenceGraphicScore; }
        public void setViolenceGraphicScore(double violenceGraphicScore) { this.violenceGraphicScore = violenceGraphicScore; }
        
        @Override
        public String toString() {
            return "ModerationResult{" +
                    "flagged=" + flagged +
                    ", hate=" + hate +
                    ", hateThreatening=" + hateThreatening +
                    ", selfHarm=" + selfHarm +
                    ", sexual=" + sexual +
                    ", sexualMinors=" + sexualMinors +
                    ", violence=" + violence +
                    ", violenceGraphic=" + violenceGraphic +
                    ", hateScore=" + hateScore +
                    ", hateThreateningScore=" + hateThreateningScore +
                    ", selfHarmScore=" + selfHarmScore +
                    ", sexualScore=" + sexualScore +
                    ", sexualMinorsScore=" + sexualMinorsScore +
                    ", violenceScore=" + violenceScore +
                    ", violenceGraphicScore=" + violenceGraphicScore +
                    '}';
        }
    }
} 