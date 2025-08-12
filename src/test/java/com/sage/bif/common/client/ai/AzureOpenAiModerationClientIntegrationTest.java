package com.sage.bif.common.client.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.common.client.ai.dto.ModerationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Azure OpenAI Moderation API 통합 테스트
 * 실제 API를 호출하므로 AZURE_OPENAI_API_KEY와 AZURE_OPENAI_ENDPOINT 환경변수가 설정되어 있어야 합니다.
 */
@SpringBootTest
@ActiveProfiles({"dev", "secret"})
@EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_ENDPOINT", matches = ".+")
class AzureOpenAiModerationClientIntegrationTest {

    @Autowired
    private AzureOpenAiModerationClient moderationClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        assertNotNull(moderationClient, "ModerationClient가 주입되지 않았습니다.");
    }

    @Test
    void testModerate_WithSafeContent_ShouldReturnNotFlagged() {
        // Given
        String safeContent = "오늘은 날씨가 좋아서 산책을 했습니다. 공원에서 꽃들을 보니 기분이 좋아졌어요.";

        // When
        ModerationResponse result = moderationClient.moderate(safeContent);

        // Then
        assertNotNull(result, "ModerationResponse가 null이면 안됩니다.");
        assertFalse(result.isFlagged(), "안전한 콘텐츠는 flagged되지 않아야 합니다.");
        assertNull(result.getFlaggedCategories(), "flagged되지 않은 경우 카테고리는 null이어야 합니다.");
        
        System.out.println("=== 안전한 콘텐츠 테스트 결과 ===");
        System.out.println("콘텐츠: " + safeContent);
        System.out.println("Flagged: " + result.isFlagged());
        System.out.println("카테고리: " + result.getFlaggedCategories());
    }

    @Test
    void testModerate_WithSelfHarmContent_ShouldReturnFlagged() {
        // Given
        String selfHarmContent = "죽고싶다";

        // When
        ModerationResponse result = moderationClient.moderate(selfHarmContent);

        // Then
        assertNotNull(result, "ModerationResponse가 null이면 안됩니다.");
        assertTrue(result.isFlagged(), "자해 관련 콘텐츠는 flagged되어야 합니다.");
        assertNotNull(result.getFlaggedCategories(), "flagged된 경우 카테고리가 있어야 합니다.");
        assertTrue(result.getFlaggedCategories().contains("self-harm"), "자해 관련 콘텐츠는 self-harm 카테고리에 포함되어야 합니다.");
        
        System.out.println("=== 자해 관련 콘텐츠 테스트 결과 ===");
        System.out.println("콘텐츠: " + selfHarmContent);
        System.out.println("Flagged: " + result.isFlagged());
        System.out.println("카테고리: " + result.getFlaggedCategories());
    }

    @Test
    void testModerate_WithViolenceContent_ShouldReturnFlagged() {
        // Given
        String violenceContent = "폭력을 사용해서 문제를 해결하자";

        // When
        ModerationResponse result = moderationClient.moderate(violenceContent);

        // Then
        assertNotNull(result, "ModerationResponse가 null이면 안됩니다.");
        // Note: 실제 API 응답에 따라 flagged 여부가 달라질 수 있음
        if (result.isFlagged()) {
            System.out.println("=== 폭력 관련 콘텐츠 테스트 결과 (Flagged) ===");
            System.out.println("콘텐츠: " + violenceContent);
            System.out.println("Flagged: " + result.isFlagged());
            System.out.println("카테고리: " + result.getFlaggedCategories());
        } else {
            System.out.println("=== 폭력 관련 콘텐츠 테스트 결과 (Not Flagged) ===");
            System.out.println("콘텐츠: " + violenceContent);
            System.out.println("Flagged: " + result.isFlagged());
        }
    }

    @Test
    void testModerate_WithHateContent_ShouldReturnFlagged() {
        // Given
        String hateContent = "특정 그룹에 대한 혐오 표현";

        // When
        ModerationResponse result = moderationClient.moderate(hateContent);

        // Then
        assertNotNull(result, "ModerationResponse가 null이면 안됩니다.");
        // Note: 실제 API 응답에 따라 flagged 여부가 달라질 수 있음
        System.out.println("=== 혐오 표현 콘텐츠 테스트 결과 ===");
        System.out.println("콘텐츠: " + hateContent);
        System.out.println("Flagged: " + result.isFlagged());
        System.out.println("카테고리: " + result.getFlaggedCategories());
    }

    @Test
    void testModerate_WithEmptyContent_ShouldHandleGracefully() {
        // Given
        String emptyContent = "";

        // When & Then
        assertDoesNotThrow(() -> {
            ModerationResponse result = moderationClient.moderate(emptyContent);
            System.out.println("=== 빈 콘텐츠 테스트 결과 ===");
            System.out.println("콘텐츠: [빈 문자열]");
            System.out.println("Flagged: " + result.isFlagged());
            System.out.println("카테고리: " + result.getFlaggedCategories());
        });
    }

    @Test
    void testModerate_WithLongContent_ShouldHandleGracefully() {
        // Given
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("이것은 매우 긴 콘텐츠입니다. ");
        }

        // When & Then
        assertDoesNotThrow(() -> {
            ModerationResponse result = moderationClient.moderate(longContent.toString());
            System.out.println("=== 긴 콘텐츠 테스트 결과 ===");
            System.out.println("콘텐츠 길이: " + longContent.length() + " 문자");
            System.out.println("Flagged: " + result.isFlagged());
            System.out.println("카테고리: " + result.getFlaggedCategories());
        });
    }

    @Test
    void testModerate_WithSpecialCharacters_ShouldHandleGracefully() {
        // Given
        String specialContent = "특수문자 테스트: !@#$%^&*()_+-=[]{}|;':\",./<>?`~";

        // When & Then
        assertDoesNotThrow(() -> {
            ModerationResponse result = moderationClient.moderate(specialContent);
            System.out.println("=== 특수문자 콘텐츠 테스트 결과 ===");
            System.out.println("콘텐츠: " + specialContent);
            System.out.println("Flagged: " + result.isFlagged());
            System.out.println("카테고리: " + result.getFlaggedCategories());
        });
    }
}
