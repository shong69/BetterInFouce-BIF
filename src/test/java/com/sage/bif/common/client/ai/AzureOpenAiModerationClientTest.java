package com.sage.bif.common.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.common.client.ai.dto.ModerationRequest;
import com.sage.bif.common.client.ai.dto.ModerationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureOpenAiModerationClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AzureOpenAiModerationClient moderationClient;

    private final String testApiKey = "test-api-key";
    private final String testEndpoint = "https://test-resource.openai.azure.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(moderationClient, "apiKey", testApiKey);
        ReflectionTestUtils.setField(moderationClient, "endpoint", testEndpoint);
    }

    @Test
    void testModerate_WithSafeContent_ShouldReturnNotFlagged() throws Exception {
        // Given
        String safeContent = "오늘은 날씨가 좋아서 산책을 했습니다.";
        String expectedEndpoint = testEndpoint + "/openai/deployments/ai-moderate-deploy/moderations?api-version=2025-04-14";
        
        ModerationRequest expectedRequest = new ModerationRequest(safeContent);
        String requestBody = "{\"input\":\"" + safeContent + "\"}";
        
        // Mock successful response
        JsonNode mockResponseBody = createMockResponseBody(false, null);
        
        when(objectMapper.writeValueAsString(any(ModerationRequest.class))).thenReturn(requestBody);
        when(restTemplate.postForEntity(eq(expectedEndpoint), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(ResponseEntity.ok(mockResponseBody));

        // When
        ModerationResponse result = moderationClient.moderate(safeContent);

        // Then
        assertNotNull(result);
        assertFalse(result.isFlagged());
        assertNull(result.getFlaggedCategories());
        
        // Verify API call
        verify(restTemplate).postForEntity(eq(expectedEndpoint), any(HttpEntity.class), eq(JsonNode.class));
        verify(objectMapper).writeValueAsString(any(ModerationRequest.class));
    }

    @Test
    void testModerate_WithFlaggedContent_ShouldReturnFlagged() throws Exception {
        // Given
        String flaggedContent = "죽고싶다";
        String expectedEndpoint = testEndpoint + "/openai/deployments/ai-moderate-deploy/moderations?api-version=2025-04-14";
        
        ModerationRequest expectedRequest = new ModerationRequest(flaggedContent);
        String requestBody = "{\"input\":\"" + flaggedContent + "\"}";
        
        // Mock flagged response
        JsonNode mockResponseBody = createMockResponseBody(true, "self-harm");
        
        when(objectMapper.writeValueAsString(any(ModerationRequest.class))).thenReturn(requestBody);
        when(restTemplate.postForEntity(eq(expectedEndpoint), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(ResponseEntity.ok(mockResponseBody));

        // When
        ModerationResponse result = moderationClient.moderate(flaggedContent);

        // Then
        assertNotNull(result);
        assertTrue(result.isFlagged());
        assertEquals("self-harm", result.getFlaggedCategories());
        
        // Verify API call
        verify(restTemplate).postForEntity(eq(expectedEndpoint), any(HttpEntity.class), eq(JsonNode.class));
        verify(objectMapper).writeValueAsString(any(ModerationRequest.class));
    }

    @Test
    void testModerate_WithMultipleFlaggedCategories_ShouldReturnAllCategories() throws Exception {
        // Given
        String flaggedContent = "폭력적인 내용과 혐오 표현";
        String expectedEndpoint = testEndpoint + "/openai/deployments/ai-moderate-deploy/moderations?api-version=2025-04-14";
        
        // Mock response with multiple flagged categories
        JsonNode mockResponseBody = createMockResponseBody(true, "violence hate");
        
        when(objectMapper.writeValueAsString(any(ModerationRequest.class))).thenReturn("{}");
        when(restTemplate.postForEntity(eq(expectedEndpoint), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(ResponseEntity.ok(mockResponseBody));

        // When
        ModerationResponse result = moderationClient.moderate(flaggedContent);

        // Then
        assertNotNull(result);
        assertTrue(result.isFlagged());
        String categories = result.getFlaggedCategories();
        assertTrue(categories.contains("violence"), "violence 카테고리가 포함되어야 합니다.");
        assertTrue(categories.contains("hate"), "hate 카테고리가 포함되어야 합니다.");
    }

    @Test
    void testModerate_WithHttpError_ShouldThrowBaseException() throws Exception {
        // Given
        String content = "테스트 내용";
        String expectedEndpoint = testEndpoint + "/openai/deployments/ai-moderate-deploy/moderations?api-version=2025-04-14";
        
        when(objectMapper.writeValueAsString(any(ModerationRequest.class))).thenReturn("{}");
        when(restTemplate.postForEntity(eq(expectedEndpoint), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        // When & Then
        assertThrows(Exception.class, () -> moderationClient.moderate(content));
    }

    @Test
    void testModerate_WithEmptyResponse_ShouldThrowBaseException() throws Exception {
        // Given
        String content = "테스트 내용";
        String expectedEndpoint = testEndpoint + "/openai/deployments/ai-moderate-deploy/moderations?api-version=2025-04-14";
        
        when(objectMapper.writeValueAsString(any(ModerationRequest.class))).thenReturn("{}");
        when(restTemplate.postForEntity(eq(expectedEndpoint), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(ResponseEntity.ok(null));

        // When & Then
        assertThrows(Exception.class, () -> moderationClient.moderate(content));
    }

    @Test
    void testModerate_VerifyHeaders() throws Exception {
        // Given
        String content = "테스트 내용";
        String expectedEndpoint = testEndpoint + "/openai/deployments/ai-moderate-deploy/moderations?api-version=2025-04-14";
        
        JsonNode mockResponseBody = createMockResponseBody(false, null);
        
        when(objectMapper.writeValueAsString(any(ModerationRequest.class))).thenReturn("{}");
        when(restTemplate.postForEntity(eq(expectedEndpoint), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(ResponseEntity.ok(mockResponseBody));

        // When
        moderationClient.moderate(content);

        // Then
        verify(restTemplate).postForEntity(
            eq(expectedEndpoint), 
            argThat(entity -> {
                HttpHeaders headers = ((HttpEntity<?>) entity).getHeaders();
                return headers.getContentType().equals(MediaType.APPLICATION_JSON) &&
                       headers.getFirst("api-key").equals(testApiKey);
            }), 
            eq(JsonNode.class)
        );
    }

    private JsonNode createMockResponseBody(boolean flagged, String categories) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // categories 문자열을 파싱하여 각 카테고리의 boolean 값을 설정
            boolean hate = categories != null && categories.contains("hate");
            boolean violence = categories != null && categories.contains("violence");
            boolean selfHarm = categories != null && categories.contains("self-harm");
            
            String jsonString = String.format(
                "{\"results\":[{\"flagged\":%s,\"categories\":{\"hate\":%s,\"hate/threatening\":false,\"self-harm\":%s,\"sexual\":false,\"sexual/minors\":false,\"violence\":%s,\"violence/graphic\":false}}]}",
                flagged,
                hate,
                selfHarm,
                violence
            );
            return mapper.readTree(jsonString);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock response body", e);
        }
    }
}
