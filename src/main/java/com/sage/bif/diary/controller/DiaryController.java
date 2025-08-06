package com.sage.bif.diary.controller;

import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.diary.dto.request.DiaryRequest;
import com.sage.bif.diary.dto.request.MonthlySummaryRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.dto.response.MonthlySummaryResponse;
import com.sage.bif.diary.model.Emotion;
import com.sage.bif.diary.service.DiaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
@Tag(name = "Diary", description = "일기 관련 API")
public class DiaryController {
    
    private final DiaryService diaryService;
    
    // 테스트 API 응답 필드 상수
    private static final String SUCCESS_FIELD = "success";
    private static final String ERROR_FIELD = "error";
    private static final String ERROR_TYPE_FIELD = "errorType";

    @GetMapping("/monthly-summary")
    @Operation(summary="월간 요약 조회", description = "감정일기 월간 요약을 조회합니다. 감정을 선택해 일기를 작성할 수 있습니다.")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummaryGet(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute MonthlySummaryRequest request) {

        MonthlySummaryResponse response = diaryService.getMonthlySummary(userDetails.getBifId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{diaryId}")
    @Operation(summary = "일기 조회", description = "지정한 일기를 조회합니다.")
    public ResponseEntity<DiaryResponse> getDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId) {
        DiaryResponse response = diaryService.getDiary(userDetails.getBifId(), diaryId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "일기 생성", description = "새로운 일기를 생성하고 AI 피드백을 제공합니다.")
    public ResponseEntity<DiaryResponse> createDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DiaryRequest request) {
        DiaryResponse response = diaryService.createDiary(userDetails.getBifId(), request);
        URI location = URI.create("/api/diaries/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{diaryId}")
    @Operation(summary = "일기 내용 수정", description = "기존 일기의 내용을 수정합니다.")
    public ResponseEntity<DiaryResponse> updateDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId, @RequestBody String content) {
        DiaryResponse response = diaryService.updateDiaryContent(userDetails.getBifId(), diaryId, content);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{diaryId}")
    @Operation(summary="일기 삭제", description = "지정한 일기를 삭제합니다.")
    public ResponseEntity<Void> deleteDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId) {
        diaryService.deleteDiary(userDetails.getBifId(), diaryId);
        return ResponseEntity.noContent().build();
    }

    // ========== 테스트용 API (인증 없음) ==========
    
    @GetMapping("/test/{diaryId}")
    @Operation(summary = "[테스트] 일기 조회", description = "인증 없이 일기를 조회합니다. (테스트용)")
    public ResponseEntity<DiaryResponse> getDiaryTest(@PathVariable Long diaryId) {
        // 테스트용 사용자 ID (1번 사용자)
        Long testUserId = 1L;
        DiaryResponse response = diaryService.getDiary(testUserId, diaryId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test")
    @Operation(summary = "[테스트] 일기 생성", description = "인증 없이 일기를 생성합니다. (테스트용)")
    public ResponseEntity<DiaryResponse> createDiaryTest(@Valid @RequestBody DiaryRequest request) {
        // 테스트용 사용자 ID (1번 사용자)
        Long testUserId = 1L;
        DiaryResponse response = diaryService.createDiary(testUserId, request);
        URI location = URI.create("/api/diaries/test/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/test/{diaryId}")
    @Operation(summary = "[테스트] 일기 내용 수정", description = "인증 없이 일기 내용을 수정합니다. (테스트용)")
    public ResponseEntity<DiaryResponse> updateDiaryTest(
            @PathVariable Long diaryId, 
            @RequestBody String content) {
        // 테스트용 사용자 ID (1번 사용자)
        Long testUserId = 1L;
        DiaryResponse response = diaryService.updateDiaryContent(testUserId, diaryId, content);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/test/{diaryId}")
    @Operation(summary = "[테스트] 일기 삭제", description = "인증 없이 일기를 삭제합니다. (테스트용)")
    public ResponseEntity<Void> deleteDiaryTest(@PathVariable Long diaryId) {
        // 테스트용 사용자 ID (1번 사용자)
        Long testUserId = 1L;
        diaryService.deleteDiary(testUserId, diaryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test/monthly-summary")
    @Operation(summary = "[테스트] 월간 요약 조회", description = "인증 없이 월간 요약을 조회합니다. (테스트용)")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummaryTest(
            @Valid @ModelAttribute MonthlySummaryRequest request) {
        // 테스트용 사용자 ID (1번 사용자)
        Long testUserId = 1L;
        MonthlySummaryResponse response = diaryService.getMonthlySummary(testUserId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test/moderation")
    @Operation(summary = "[테스트] Moderation 테스트", description = "Moderation API를 직접 테스트합니다. (테스트용)")
    public ResponseEntity<Map<String, Object>> testModeration(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put(SUCCESS_FIELD, false);
            result.put(ERROR_FIELD, "content 필드가 필요합니다.");
            result.put(ERROR_TYPE_FIELD, "ValidationException");
            return ResponseEntity.badRequest().body(result);
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // DiaryRequest 생성
            DiaryRequest diaryRequest = DiaryRequest.builder()
                .content(content)
                .emotion(Emotion.JOY) // 기본값
                .date(LocalDateTime.now())
                .build();
            
            // 일기 생성 (moderation 체크 포함)
            Long testUserId = 1L;
            DiaryResponse response = diaryService.createDiary(testUserId, diaryRequest);
            
            result.put(SUCCESS_FIELD, true);
            result.put("diaryId", response.getId());
            result.put("contentFlagged", response.isContentFlagged());
            result.put("contentFlaggedCategories", response.getContentFlaggedCategories());
            result.put("aiFeedback", response.getAiFeedback());
            result.put("message", response.isContentFlagged() ? 
                "부적절한 콘텐츠가 감지되었습니다." : "콘텐츠가 정상입니다.");
            
        } catch (Exception e) {
            result.put(SUCCESS_FIELD, false);
            result.put(ERROR_FIELD, e.getMessage());
            result.put(ERROR_TYPE_FIELD, e.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/test/list")
    @Operation(summary = "[테스트] 일기 목록 조회", description = "인증 없이 일기 목록을 조회합니다. (테스트용)")
    public ResponseEntity<Map<String, Object>> getDiaryListTest() {
        // 테스트용 사용자 ID (1번 사용자)
        Long testUserId = 1L;
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 현재 월의 요약 조회
            MonthlySummaryRequest request = MonthlySummaryRequest.ofCurrentMonth(testUserId);
            
            MonthlySummaryResponse summary = diaryService.getMonthlySummary(testUserId, request);
            
            result.put(SUCCESS_FIELD, true);
            result.put("year", request.getYear());
            result.put("month", request.getMonth());
            result.put("daiyDailyEmotions", summary.getDailyEmotions());
            result.put("dailyEmotionsCount", summary.getDailyEmotions().size());

        } catch (Exception e) {
            result.put(SUCCESS_FIELD, false);
            result.put(ERROR_FIELD, e.getMessage());
            result.put(ERROR_TYPE_FIELD, e.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(result);
    }

} 