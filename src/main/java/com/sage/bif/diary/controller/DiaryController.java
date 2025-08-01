package com.sage.bif.diary.controller;

import com.sage.bif.diary.dto.request.DiaryCreateRequest;
import com.sage.bif.diary.dto.request.MonthlySummaryRequest;
import com.sage.bif.diary.dto.response.DiaryResponse;
import com.sage.bif.diary.dto.response.MonthlySummaryResponse;
import com.sage.bif.diary.service.DiaryService;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.diary.model.Emotion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
@Tag(name = "Diary", description = "일기 관련 API")
public class DiaryController {

    private final DiaryService diaryService;

    @GetMapping("/monthly-summary")
    @Operation(summary="월간 요약 조회 (GET)", description = "감정일기 월간 요약을 조회합니다. 감정을 선택해 일기를 작성할 수 있습니다.")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummaryGet(@Valid @ModelAttribute MonthlySummaryRequest request) {
        MonthlySummaryResponse response = diaryService.getMonthlySummary(request);
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/{diaryId}")
//    @Operation(summary = "일기 조회", description = "지정한 일기를 조회합니다.")
//    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Long diaryId) {
//        DiaryResponse response = diaryService.getDiary(diaryId);
//        return ResponseEntity.ok(response);
//    }

    @PostMapping
    @Operation(summary = "일기 생성", description = "새로운 일기를 생성하고 AI 피드백을 제공합니다.")
    public ResponseEntity<DiaryResponse> createDiary(@Valid @RequestBody DiaryCreateRequest request) {
        DiaryResponse response = diaryService.createDiary(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{diaryId}")
    @Operation(summary = "일기 내용 수정", description = "기존 일기의 내용을 수정합니다.")
    public ResponseEntity<DiaryResponse> updateDiary(@PathVariable Long diaryId, @RequestBody String content) {
        DiaryResponse response = diaryService.updateDiaryContent(diaryId, content);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{diaryId}")
    @Operation(summary="일기 삭제", description = "지정한 일기를 삭제합니다.")
    public void deleteDiary(@PathVariable Long diaryId) {
        diaryService.deleteDiary(diaryId);
    }
} 