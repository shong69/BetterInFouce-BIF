package com.sage.bif.common.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sage.bif.common.client.ai.dto.AiChatSettings;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
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
 * Azure OpenAI 클라이언트 구현체 (HTTP 직접 호출)
 * 동적 파라미터 설정 지원
 */
@Component
public class AzureOpenAiClient implements AiServiceClient {
    
    @Value("${spring.ai.azure.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.azure.openai.endpoint}")
    private String endpoint;
    
    @Value("${spring.ai.azure.openai.deployment-name}")
    private String deploymentName;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AzureOpenAiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        // 기본 설정 사용
        return generate(request, AiChatSettings.getDefault());
    }

    @Override
    public AiResponse generate(AiRequest request, AiChatSettings settings) {
        // 설정에서 값들을 추출해서 내부 메서드 호출
        return chat(request, settings.getSystemPrompt(), settings.getTemperature(), settings.getMaxTokens());
    }
    
    /**
     * 내부 구현 메서드 - 복잡한 HTTP 처리 로직
     * 
     * @param request 사용자 요청 (userPrompt만 포함)
     * @param systemPrompt 시스템 프롬프트
     * @param temperature 창의성 조절 (0.0 ~ 2.0)
     * @param maxTokens 최대 토큰 수
     * @return AI 응답
     */
    private AiResponse chat(AiRequest request, String systemPrompt, double temperature, int maxTokens) {
        try {
            String chatEndpoint = String.format("%s/openai/deployments/%s/chat/completions?api-version=2025-01-01-preview", 
                endpoint, deploymentName);
            
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            
            // 요청 본문 생성
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            
            ArrayNode messages = objectMapper.createArrayNode();
            
            // 시스템 메시지 추가
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
            
            // 사용자 메시지 추가
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", request.getUserPrompt());
            messages.add(userMessage);
            
            requestBody.set("messages", messages);
            
            // HTTP 요청 전송
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(chatEndpoint, entity, JsonNode.class);
            
            // 응답 검증 및 내용 추출
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
}