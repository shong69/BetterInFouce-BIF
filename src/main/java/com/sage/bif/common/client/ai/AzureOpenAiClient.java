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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class AzureOpenAiClient implements AiServiceClient {

    private static final String CONTENT_FIELD = "content";
    private static final String CHOICES_FIELD = "choices";
    private static final String MESSAGE_FIELD = "message";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${spring.ai.azure.openai.api-key}")
    private String apiKey;
    @Value("${spring.ai.azure.openai.endpoint}")
    private String endpoint;
    @Value("${spring.ai.azure.openai.deployment.name}")
    private String deploymentName;

    public AzureOpenAiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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
            systemMessage.put(CONTENT_FIELD, systemPrompt);
            messages.add(systemMessage);

            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put(CONTENT_FIELD, request.getUserPrompt());
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

            if (body.has(CHOICES_FIELD) && body.get(CHOICES_FIELD).isArray() && !body.get(CHOICES_FIELD).isEmpty()) {
                JsonNode firstChoice = body.get(CHOICES_FIELD).get(0);

                if (firstChoice.has(MESSAGE_FIELD) && firstChoice.get(MESSAGE_FIELD).has(CONTENT_FIELD)) {
                    String content = firstChoice.get(MESSAGE_FIELD).get(CONTENT_FIELD).asText();
                    return new AiResponse(content);
                }
            }

            throw new BaseException(ErrorCode.COMMON_AI_RESPONSE_INVALID,
                    "응답 형식이 올바르지 않습니다. 응답 본문: " + body);
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE,
                        "API 키 인증 실패: " + e.getMessage());
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new BaseException(ErrorCode.COMMON_AI_QUOTA_EXCEEDED,
                        "API 호출 한도 초과: " + e.getMessage());
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new BaseException(ErrorCode.COMMON_AI_REQUEST_FAILED,
                        "Azure OpenAI 콘텐츠 정책 위반: " + responseBody);
            } else {
                throw new BaseException(ErrorCode.COMMON_AI_REQUEST_FAILED,
                        "HTTP 클라이언트 오류: " + e.getMessage());
            }
        } catch (HttpServerErrorException e) {
            throw new BaseException(ErrorCode.COMMON_AI_SERVICE_UNAVAILABLE,
                    "AI 서비스 내부 오류: " + e.getMessage());
        } catch (Exception e) {
            throw new BaseException(ErrorCode.COMMON_AI_MODEL_ERROR,
                    "예상치 못한 오류: " + e.getMessage());
        }
    }

}
