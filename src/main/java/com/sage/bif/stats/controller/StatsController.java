package com.sage.bif.stats.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "감정 통계 관련 API")
public class StatsController {

    private final StatsService statsService;

    @Operation(
        summary = "월별 감정 통계 조회",
        description = "BIF 사용자의 현재 월 감정 통계를 조회합니다. 감정 분석 텍스트, 감정 비율, 상위 키워드, 월별 변화 데이터를 포함합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "월별 감정 통계 조회 성공",
            content = @Content(schema = @Schema(implementation = StatsResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/stat")
    public ResponseEntity<com.sage.bif.common.dto.ApiResponse<StatsResponse>> getMonthlyStats(
            // 실서비스용: @AuthenticationPrincipal final UserDetails userDetails
            @Parameter(description = "테스트용 사용자명 (개발용)", required = false)
            @RequestParam(value = "username", defaultValue = "test_user") final String username) {

        log.info("월별 감정 통계 조회 - 사용자: {}", username);

        final StatsResponse statsResponse = statsService.getMonthlyStats(username);

        return ResponseEntity.ok(com.sage.bif.common.dto.ApiResponse.<StatsResponse>builder()
                .success(true)
                .message("월별 감정 통계 조회 성공")
                .data(statsResponse)
                .build());
    }

    @Operation(
        summary = "보호자용 BIF 통계 조회",
        description = "보호자가 연결된 BIF의 감정 상태를 모니터링할 수 있는 통계를 조회합니다. BIF 닉네임, 맞춤형 조언, 감정 비율, 월별 변화 데이터를 포함합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "보호자용 BIF 통계 조회 성공",
            content = @Content(schema = @Schema(implementation = GuardianStatsResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/guardian/stat")
    public ResponseEntity<com.sage.bif.common.dto.ApiResponse<GuardianStatsResponse>> getGuardianStats(
            // 실서비스용: @AuthenticationPrincipal final UserDetails userDetails
            @Parameter(description = "테스트용 보호자명 (개발용)", required = false)
            @RequestParam(value = "username", defaultValue = "test_guardian") final String username) {

        log.info("보호자용 BIF 통계 조회 - 사용자: {}", username);

        final GuardianStatsResponse guardianStatsResponse = statsService.getGuardianStats(username);

        return ResponseEntity.ok(com.sage.bif.common.dto.ApiResponse.<GuardianStatsResponse>builder()
                .success(true)
                .message("보호자용 BIF 통계 조회 성공")
                .data(guardianStatsResponse)
                .build());
    }




    
    // ===== 테스트용 엔드포인트들 (개발/테스트 시에만 사용) =====
    // 정상 서비스 시에는 주석 해제하고 삭제
    
    @Operation(
        summary = "테스트용 월별 감정 통계 조회",
        description = "테스트를 위한 더미 BIF ID로 월별 감정 통계를 조회합니다. (개발/테스트용)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "테스트용 월별 감정 통계 조회 성공",
            content = @Content(schema = @Schema(implementation = StatsResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/test/stat/{bifId}")
    public ResponseEntity<com.sage.bif.common.dto.ApiResponse<StatsResponse>> getTestMonthlyStats(
            @Parameter(description = "테스트용 BIF ID (1, 2, 3 중 선택)", required = true)
            @PathVariable final Long bifId) {

        log.info("테스트용 월별 감정 통계 조회 - BIF ID: {}", bifId);

        // 테스트용 더미 사용자명 생성
        final String dummyUsername = "test_bif_" + bifId;
        final StatsResponse statsResponse = statsService.getMonthlyStats(dummyUsername);

        return ResponseEntity.ok(com.sage.bif.common.dto.ApiResponse.<StatsResponse>builder()
                .success(true)
                .message("테스트용 월별 감정 통계 조회 성공")
                .data(statsResponse)
                .build());
    }

    @Operation(
        summary = "테스트용 보호자 통계 조회",
        description = "테스트를 위한 더미 보호자 ID로 연결된 BIF의 통계를 조회합니다. (개발/테스트용)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "테스트용 보호자 통계 조회 성공",
            content = @Content(schema = @Schema(implementation = GuardianStatsResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/test/guardian/stat/{guardianId}")
    public ResponseEntity<com.sage.bif.common.dto.ApiResponse<GuardianStatsResponse>> getTestGuardianStats(
            @Parameter(description = "테스트용 보호자 ID (2001, 2002, 2003 중 선택)", required = true)
            @PathVariable final Long guardianId) {

        log.info("테스트용 보호자 통계 조회 - 보호자 ID: {}", guardianId);

        // 테스트용 더미 보호자명 생성
        final String dummyGuardianUsername = "test_guardian_" + guardianId;
        final GuardianStatsResponse guardianStatsResponse = statsService.getGuardianStats(dummyGuardianUsername);

        return ResponseEntity.ok(com.sage.bif.common.dto.ApiResponse.<GuardianStatsResponse>builder()
                .success(true)
                .message("테스트용 보호자 통계 조회 성공")
                .data(guardianStatsResponse)
                .build());
    }
    

}
