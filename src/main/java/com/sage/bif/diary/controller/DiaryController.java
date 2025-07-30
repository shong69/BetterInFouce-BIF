package com.sage.bif.diary.controller;

import com.sage.bif.common.client.ai.AiServiceClient;
import com.sage.bif.common.client.ai.AiSettings;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
import com.sage.bif.diary.dto.request.DiaryCreateRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
@Tag(name = "Diary", description = "일기 관련 API")
public class DiaryController {

    private final DiaryService diaryService;
    private final AiServiceClient aiServiceClient;

    @PostMapping
    @Operation(summary = "일기 생성", description = "새로운 일기를 생성하고 AI 피드백을 제공합니다.")
    public ResponseEntity<DiaryResponse> createDiary(@Valid @RequestBody DiaryCreateRequest request) {
        DiaryResponse response = diaryService.createDiary(request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/azure-key")
    public String checkAzureKey() {
        String apiKey = System.getenv("AZURE_OPENAI_API_KEY");
        System.out.println("AZURE_OPENAI_API_KEY = " + apiKey);  // 임시 확인용
        return apiKey != null ? "OK: " + apiKey : "환경변수 없음";
    }
    @GetMapping("/test")
    @Operation(summary = "AI 응답 테스트", description = "기본 일기 피드백 AI 응답을 테스트합니다.")
    public ResponseEntity<String> testAiResponse(){
        try {
            AiRequest request = new AiRequest("오늘은 너무 재밋는 하루였다~~");
            AiResponse response = aiServiceClient.generate(request, AiSettings.DIARY_FEEDBACK);
            return ResponseEntity.ok("AI 응답: " + response.getContent());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("AI 응답 오류: " + e.getMessage());
        }
    }

    @PostMapping("/test-ai")
    @Operation(summary = "커스텀 AI 응답 테스트", description = "사용자 입력에 대한 AI 응답을 테스트합니다.")
    public ResponseEntity<String> testCustomAiResponse(@RequestBody String userInput){
        try {
            AiRequest request = new AiRequest(userInput);
            AiResponse response = aiServiceClient.generate(request, AiSettings.DIARY_FEEDBACK);
            return ResponseEntity.ok("AI 응답: " + response.getContent());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("AI 응답 오류: " + e.getMessage());
        }
    }

    @GetMapping("/test-ai-settings")
    @Operation(summary = "AI 설정 테스트", description = "다양한 AI 설정으로 응답을 테스트합니다.")
    public ResponseEntity<String> testAiSettings(){
        try {
            AiRequest request = new AiRequest("오늘은 스트레스가 많았지만 새로운 것을 배웠다.");
            
            // 기본 설정으로 테스트
            AiResponse defaultResponse = aiServiceClient.generate(request);
            
            // 일기 피드백 설정으로 테스트
            AiResponse diaryResponse = aiServiceClient.generate(request, AiSettings.DIARY_FEEDBACK);
            
            // 할일 우선순위 설정으로 테스트
            AiResponse todoResponse = aiServiceClient.generate(request, AiSettings.TODO_PRIORITY);
            
            StringBuilder result = new StringBuilder();
            result.append("=== 기본 설정 응답 ===\n").append(defaultResponse.getContent()).append("\n\n");
            result.append("=== 일기 피드백 응답 ===\n").append(diaryResponse.getContent()).append("\n\n");
            result.append("=== 할일 우선순위 응답 ===\n").append(todoResponse.getContent());
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("AI 설정 테스트 오류: " + e.getMessage());
        }
    }

    @GetMapping("/test-connection")
    @Operation(summary = "AI 서비스 연결 테스트", description = "AI 서비스 연결 상태를 확인합니다.")
    public ResponseEntity<String> testAiConnection(){
        try {
            // 간단한 테스트 요청
            AiRequest request = new AiRequest("안녕하세요");
            AiResponse response = aiServiceClient.generate(request);
            return ResponseEntity.ok("AI 서비스 연결 성공! 응답: " + response.getContent());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("AI 서비스 연결 실패: " + e.getMessage());
        }
    }
} 