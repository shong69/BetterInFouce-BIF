package com.sage.bif.common.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sage.bif.common.client.ai.dto.AiChatSettings;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ContentModerationException;
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

@Component
public class AzureOpenAiClient implements AiServiceClient {
    
    @Value("${spring.ai.azure.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.azure.openai.endpoint}")
    private String endpoint;
    
    @Value("${AZURE_OPENAI_DEPLOYMENT_NAME}")
    private String deploymentName;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AzureOpenAiModerationClient moderationClient;

    public AzureOpenAiClient(RestTemplate restTemplate, ObjectMapper objectMapper, AzureOpenAiModerationClient moderationClient) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.moderationClient = moderationClient;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        return generate(request, AiChatSettings.getDefault());
    }

    @Override
    public AiResponse generate(AiRequest request, AiChatSettings settings) {
        return chat(request, settings.getSystemPrompt(), settings.getTemperature(), settings.getMaxTokens());
    }

    private AiResponse chat(AiRequest request, String systemPrompt, double temperature, int maxTokens) {
        try {
            // 사용자 입력에 대한 위험도 검사
            performContentModeration(request.getUserPrompt());
            
            String chatEndpoint = String.format("%s/openai/deployments/%s/chat/completions?api-version=2025-01-01-preview", 
                endpoint, deploymentName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            
            ArrayNode messages = objectMapper.createArrayNode();
            
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
            
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", request.getUserPrompt());
            messages.add(userMessage);
            
            requestBody.set("messages", messages);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(chatEndpoint, entity, JsonNode.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE, 
                    "HTTP 상태 코드: " + response.getStatusCode());
            }
            
            JsonNode body = response.getBody();
            if (body == null) {
                throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID, 
                    "AI 서비스 응답 본문이 비어있습니다.");
            }
            
            if (body.has("choices") && body.get("choices").isArray() && !body.get("choices").isEmpty()) {
                JsonNode firstChoice = body.get("choices").get(0);
                
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    String content = firstChoice.get("message").get("content").asText();
                    
                    // AI 응답에 대한 위험도 검사
                    performContentModeration(content);
                    
                    return new AiResponse(content);
                }
            }
            throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID, 
                "응답 형식이 올바르지 않습니다. 응답 본문: " + body.toString());
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE, 
                    "API 키 인증 실패: " + e.getMessage());
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new BaseException(ErrorCode.COMMON_AI_QUOTA_EXCEEDED, 
                    "API 호출 한도 초과: " + e.getMessage());
            } else {
                throw new BaseException(ErrorCode.COMMON_AI_REQUEST_FAILED, 
                    "HTTP 클라이언트 오류: " + e.getMessage());
            }
        } catch (HttpServerErrorException e) {
            throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE, 
                "AI 서비스 내부 오류: " + e.getMessage());
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            throw new BaseException(ErrorCode.COMMON_AI_MODEL_ERROR, 
                "예상치 못한 오류: " + e.getMessage());
        }
    }
    
    /**
     * 콘텐츠의 위험도를 검사합니다.
     * @param content 검사할 콘텐츠
     * @throws ContentModerationException 위험도가 높은 경우
     */
    private void performContentModeration(String content) {
        try {
            var moderationResult = moderationClient.moderateText(content);
            
            if (moderationResult.isFlagged()) {
                log.warn("Content moderation failed for input: {}", content);
                log.warn("Moderation result: {}", moderationResult);
                throw new ContentModerationException(moderationResult);
            }
            
            log.debug("Content moderation passed for input: {}", content);
            
        } catch (ContentModerationException e) {
            // ContentModerationException은 그대로 재발생
            throw e;
        } catch (Exception e) {
            // moderation API 호출 실패 시 설정에 따라 처리
            if (moderationClient.getModerationConfig().isBlockOnFailure()) {
                log.error("Content moderation API call failed and blockOnFailure is enabled: {}", e.getMessage(), e);
                throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE, 
                    "콘텐츠 위험도 검사에 실패했습니다: " + e.getMessage());
            } else {
                log.warn("Content moderation API call failed but blockOnFailure is disabled, continuing: {}", e.getMessage());
            }
        }
    }
}