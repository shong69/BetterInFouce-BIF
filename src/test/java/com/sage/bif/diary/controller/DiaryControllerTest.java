package com.sage.bif.diary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.model.Emotion;
import com.sage.bif.diary.service.DiaryService;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DiaryControllerTest {

    @Mock
    private DiaryService diaryService;

    @Mock
    private BifRepository bifRepository;

    @Mock
    private com.sage.bif.common.client.ai.AiServiceClient aiServiceClient;

    @InjectMocks
    private DiaryController diaryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(diaryController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("정상적인 일기 생성 테스트")
    void createTestDiary_Success() throws Exception {
        // given
        String content = "오늘은 정말 좋은 하루였어요!";
        Emotion emotion = Emotion.JOY;
        Long userId = 1L;

        Bif mockUser = Bif.builder()
                .bifId(userId)
                .nickname("테스트사용자")
                .connectionCode("TEST01")
                .build();

        DiaryResponse mockResponse = DiaryResponse.builder()
                .id(1L)
                .content(content)
                .emotion(emotion)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        when(bifRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
        when(diaryService.createDiary(any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.emotion").value(emotion.name()))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    @DisplayName("다양한 감정으로 일기 생성 테스트")
    void createTestDiary_WithDifferentEmotions() throws Exception {
        // given
        String content = "오늘은 정말 화가 났어요!";
        Emotion emotion = Emotion.ANGER;
        Long userId = 2L;

        Bif mockUser = Bif.builder()
                .bifId(userId)
                .nickname("테스트사용자2")
                .connectionCode("TEST02")
                .build();

        DiaryResponse mockResponse = DiaryResponse.builder()
                .id(2L)
                .content(content)
                .emotion(emotion)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        when(bifRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
        when(diaryService.createDiary(any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotion").value(emotion.name()));
    }

    @Test
    @DisplayName("한글 특수문자가 포함된 일기 생성 테스트")
    void createTestDiary_WithKoreanSpecialCharacters() throws Exception {
        // given
        String content = "\"재미없어!!!!!!!!!\"";
        Emotion emotion = Emotion.ANGER;
        Long userId = 3L;

        Bif mockUser = Bif.builder()
                .bifId(userId)
                .nickname("테스트사용자3")
                .connectionCode("TEST03")
                .build();

        DiaryResponse mockResponse = DiaryResponse.builder()
                .id(3L)
                .content(content)
                .emotion(emotion)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        when(bifRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
        when(diaryService.createDiary(any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 일기 생성 시 오류 테스트")
    void createTestDiary_UserNotFound() throws Exception {
        // given
        String content = "테스트 일기";
        Emotion emotion = Emotion.JOY;
        Long userId = 999L;

        when(bifRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("잘못된 감정 값으로 일기 생성 시 오류 테스트")
    void createTestDiary_InvalidEmotion() throws Exception {
        // given
        String content = "테스트 일기";
        String invalidEmotion = "INVALID_EMOTION";
        Long userId = 1L;

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", invalidEmotion)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException));
    }

    @Test
    @DisplayName("빈 내용으로 일기 생성 시 오류 테스트")
    void createTestDiary_EmptyContent() throws Exception {
        // given
        String content = "";
        Emotion emotion = Emotion.JOY;
        Long userId = 1L;

        Bif mockUser = Bif.builder()
                .bifId(userId)
                .nickname("테스트사용자")
                .connectionCode("TEST01")
                .build();

        DiaryResponse mockResponse = DiaryResponse.builder()
                .id(1L)
                .content(content)
                .emotion(emotion)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        when(bifRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
        when(diaryService.createDiary(any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // 현재 구현에서는 빈 내용도 허용됨
    }

    @Test
    @DisplayName("userId 파라미터가 없을 때 오류 테스트")
    void createTestDiary_MissingUserId() throws Exception {
        // given
        String content = "테스트 일기";
        Emotion emotion = Emotion.JOY;

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof org.springframework.web.bind.MissingServletRequestParameterException));
    }

    @Test
    @DisplayName("모든 감정 타입으로 일기 생성 테스트")
    void createTestDiary_AllEmotionTypes() throws Exception {
        Emotion[] emotions = {Emotion.EXCELLENT, Emotion.JOY, Emotion.NEUTRAL, Emotion.SAD, Emotion.ANGER};
        
        for (Emotion emotion : emotions) {
            // given
            String content = emotion.name() + " 감정의 일기";
            Long userId = 1L;

            Bif mockUser = Bif.builder()
                    .bifId(userId)
                    .nickname("테스트사용자")
                    .connectionCode("TEST01")
                    .build();

            DiaryResponse mockResponse = DiaryResponse.builder()
                    .id(1L)
                    .content(content)
                    .emotion(emotion)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(bifRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
            when(diaryService.createDiary(any())).thenReturn(mockResponse);

            // when & then
            mockMvc.perform(post("/diaries/test/create")
                            .param("content", content)
                            .param("emotion", emotion.name())
                            .param("userId", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emotion").value(emotion.name()));
        }
    }

    @Test
    @DisplayName("AI 피드백이 포함된 일기 생성 테스트")
    void createTestDiary_WithAiFeedback() throws Exception {
        // given
        String content = "오늘은 정말 좋은 하루였어요! 친구들과 함께 놀았고, 맛있는 음식도 먹었어요.";
        Emotion emotion = Emotion.JOY;
        Long userId = 1L;
        String expectedAiFeedback = "정말 즐거운 하루를 보내셨네요! 친구들과의 시간은 소중한 추억이 될 거예요. 이런 긍정적인 경험들이 당신의 삶을 더욱 풍요롭게 만들어줄 것입니다.";

        Bif mockUser = Bif.builder()
                .bifId(userId)
                .nickname("테스트사용자")
                .connectionCode("TEST01")
                .build();

        DiaryResponse mockResponse = DiaryResponse.builder()
                .id(1L)
                .content(content)
                .emotion(emotion)
                .userId(userId)
                .aiFeedback(expectedAiFeedback)
                .createdAt(LocalDateTime.now())
                .build();

        when(bifRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
        when(diaryService.createDiary(any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.emotion").value(emotion.name()))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.aiFeedback").value(expectedAiFeedback))
                .andExpect(jsonPath("$.aiFeedback").exists())
                .andExpect(jsonPath("$.aiFeedback").isNotEmpty());
    }

    @Test
    @DisplayName("AI 피드백이 null인 일기 생성 테스트")
    void createTestDiary_WithNullAiFeedback() throws Exception {
        // given
        String content = "오늘은 그냥 평범한 하루였어요.";
        Emotion emotion = Emotion.NEUTRAL;
        Long userId = 1L;

        Bif mockUser = Bif.builder()
                .bifId(userId)
                .nickname("테스트사용자")
                .connectionCode("TEST01")
                .build();

        DiaryResponse mockResponse = DiaryResponse.builder()
                .id(1L)
                .content(content)
                .emotion(emotion)
                .userId(userId)
                .aiFeedback(null)
                .createdAt(LocalDateTime.now())
                .build();

        when(bifRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
        when(diaryService.createDiary(any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.emotion").value(emotion.name()))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.aiFeedback").isEmpty());
    }

    @Test
    @DisplayName("AI 피드백 생성 실패 시 일기 생성 테스트")
    void createTestDiary_AiFeedbackFailure() throws Exception {
        // given
        String content = "오늘은 정말 좋은 하루였어요!";
        Emotion emotion = Emotion.JOY;
        Long userId = 1L;

        Bif mockUser = Bif.builder()
                .bifId(userId)
                .nickname("테스트사용자")
                .connectionCode("TEST01")
                .build();

        // AI 피드백 생성 실패 시나리오
        DiaryResponse mockResponse = DiaryResponse.builder()
                .id(1L)
                .content(content)
                .emotion(emotion)
                .userId(userId)
                .aiFeedback("AI 피드백 생성에 실패했습니다.")
                .createdAt(LocalDateTime.now())
                .build();

        when(bifRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
        when(diaryService.createDiary(any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/diaries/test/create")
                        .param("content", content)
                        .param("emotion", emotion.name())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.emotion").value(emotion.name()))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.aiFeedback").value("AI 피드백 생성에 실패했습니다."));
    }
} 