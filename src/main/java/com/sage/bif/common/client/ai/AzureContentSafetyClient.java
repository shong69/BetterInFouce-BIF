package com.sage.bif.common.client.ai;

import com.sage.bif.common.client.ai.dto.ModerationRequest;
import com.sage.bif.common.client.ai.dto.ModerationResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collections;

@Slf4j
@Component
public class AzureContentSafetyClient {

    @Value("${spring.ai.azure.content-safety.endpoint:}")
    private String endpoint;
    
    @Value("${spring.ai.azure.content-safety.api-key:}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    
    public AzureContentSafetyClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ModerationResponse moderateText(String text) {
        if (!isConfigured()) {
            log.warn("Azure Content Safety API가 설정되지 않았습니다.");
            return createDefaultResponse();
        }
        
        try {
            ModerationRequest request = new ModerationRequest(text);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<ModerationRequest> entity = new HttpEntity<>(request, headers);

            String url = endpoint + "/contentsafety/text:analyze?api-version=2024-09-01";

            log.info("Azure Content Safety API 호출 시작: {}", url);
            
            ResponseEntity<ModerationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ModerationResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Azure Content Safety API 호출 성공");
                return response.getBody();
            } else {
                log.error("Azure Content Safety API 응답이 비어있거나 실패: {}", response.getStatusCode());
                return createDefaultResponse();
            }
            
        } catch (HttpClientErrorException e) {
            log.error("Azure Content Safety API HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createDefaultResponse();
        } catch (ResourceAccessException e) {
            log.error("Azure Content Safety API 연결 오류: {}", e.getMessage());
            return createDefaultResponse();
        } catch (Exception e) {
            log.error("Azure Content Safety API 예상치 못한 오류: {}", e.getMessage(), e);
            return createDefaultResponse();
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Ocp-Apim-Subscription-Key", apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    public boolean isConfigured() {
        return endpoint != null && !endpoint.trim().isEmpty() 
            && apiKey != null && !apiKey.trim().isEmpty();
    }

    private ModerationResponse createDefaultResponse() {
        ModerationResponse response = new ModerationResponse();
        response.setCategoriesAnalysis(null);
        return response;
    }
    
}
