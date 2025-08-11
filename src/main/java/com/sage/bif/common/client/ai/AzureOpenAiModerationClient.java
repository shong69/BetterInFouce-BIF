package com.sage.bif.common.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.common.client.ai.dto.ModerationRequest;
import com.sage.bif.common.client.ai.dto.ModerationResponse;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
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

/**
 * Azure OpenAI Moderation API 클라이언트
 */
@Component
public class AzureOpenAiModerationClient {
    
    // API 응답 필드 상수
    private static final String RESULTS_FIELD = "results";
    private static final String FLAGGED_FIELD = "flagged";
    private static final String CATEGORIES_FIELD = "categories";
    
    @Value("${spring.ai.azure.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.azure.openai.resource-name}")
    private String resourceName;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AzureOpenAiModerationClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ModerationResponse moderate(String input) {
        try {
            String moderationEndpoint = String.format("https://%s.openai.azure.com/openai/moderations?api-version=2025-01-01-preview", resourceName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            
            ModerationRequest moderationRequest = new ModerationRequest(input);
            String requestBody = objectMapper.writeValueAsString(moderationRequest);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(moderationEndpoint, entity, JsonNode.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE, 
                    "Moderation API 호출 실패. HTTP 상태 코드: " + response.getStatusCode());
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

    /**
     * Moderation API 응답을 파싱하여 ModerationResponse 객체로 변환합니다.
     */
    private ModerationResponse parseModerationResponse(JsonNode body) {
        try {
            validateResponseFormat(body);
            
            JsonNode result = body.get(RESULTS_FIELD).get(0);
            boolean flagged = result.get(FLAGGED_FIELD).asBoolean();
            
            ModerationResponse response = new ModerationResponse();
            response.setFlagged(flagged);
            
            if (flagged && result.has(CATEGORIES_FIELD)) {
                String flaggedCategories = extractFlaggedCategories(result.get(CATEGORIES_FIELD));
                response.setFlaggedCategories(flaggedCategories);
            }
            
            return response;
            
        } catch (Exception e) {
            throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID, 
                "Moderation API 응답 파싱 오류: " + e.getMessage());
        }
    }

    /**
     * 응답 형식이 올바른지 검증합니다.
     */
    private void validateResponseFormat(JsonNode body) {
        if (!body.has(RESULTS_FIELD) || !body.get(RESULTS_FIELD).isArray() || body.get(RESULTS_FIELD).isEmpty()) {
            throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID, 
                "Moderation API 응답 형식이 올바르지 않습니다.");
        }
    }

    /**
     * flagged된 카테고리들을 추출합니다.
     */
    private String extractFlaggedCategories(JsonNode categories) {
        StringBuilder flaggedCategories = new StringBuilder();
        
        // 각 카테고리를 확인하여 flagged된 것들을 수집
        if (categories.get("hate").asBoolean()) flaggedCategories.append("hate ");
        if (categories.get("hate/threatening").asBoolean()) flaggedCategories.append("hate/threatening ");
        if (categories.get("self-harm").asBoolean()) flaggedCategories.append("self-harm ");
        if (categories.get("sexual").asBoolean()) flaggedCategories.append("sexual ");
        if (categories.get("sexual/minors").asBoolean()) flaggedCategories.append("sexual/minors ");
        if (categories.get("violence").asBoolean()) flaggedCategories.append("violence ");
        if (categories.get("violence/graphic").asBoolean()) flaggedCategories.append("violence/graphic ");
        
        return flaggedCategories.toString().trim();
    }
} 