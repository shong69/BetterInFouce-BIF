package com.sage.bif.stats.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/stat")
    public ResponseEntity<ApiResponse<StatsResponse>> getMonthlyStats(
            @AuthenticationPrincipal final UserDetails userDetails) {

        log.info("월별 감정 통계 조회 - 사용자: {}", userDetails.getUsername());

        final StatsResponse statsResponse = statsService.getMonthlyStats(userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.<StatsResponse>builder()
                .success(true)
                .message("월별 감정 통계 조회 성공")
                .data(statsResponse)
                .build());
    }

    @GetMapping("/guardian/stat")
    public ResponseEntity<ApiResponse<GuardianStatsResponse>> getGuardianStats(
            @AuthenticationPrincipal final UserDetails userDetails) {

        log.info("보호자용 BIF 통계 조회 - 사용자: {}", userDetails.getUsername());

        final GuardianStatsResponse guardianStatsResponse = statsService.getGuardianStats(userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.<GuardianStatsResponse>builder()
                .success(true)
                .message("보호자용 BIF 통계 조회 성공")
                .data(guardianStatsResponse)
                .build());
    }
}
