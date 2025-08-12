package com.sage.bif.diary.controller;

import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.diary.dto.request.DiaryRequest;
import com.sage.bif.diary.dto.request.DiaryUpdateRequest;
import com.sage.bif.diary.dto.request.MonthlySummaryRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.dto.response.MonthlySummaryResponse;
import com.sage.bif.diary.service.DiaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.net.URI;
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
            @PathVariable Long diaryId, @RequestBody DiaryUpdateRequest request) {
        DiaryResponse response = diaryService.updateDiaryContent(userDetails.getBifId(), diaryId, request.getContent());
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

}
