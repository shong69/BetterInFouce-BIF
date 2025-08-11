package com.sage.bif.stats.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.service.StatsService;
import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "통계 관련 API")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/stat")
    @Operation(summary = "월별 통계 조회", description = "BIF 사용자의 월별 감정 통계를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = StatsResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<StatsResponse>> getMonthlyStats(
            @AuthenticationPrincipal final UserDetails userDetails) {

        // UserDetails를 CustomUserDetails로 캐스팅
        if (!(userDetails instanceof CustomUserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증 정보가 올바르지 않습니다."));
        }
        
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        // BIF 사용자만 접근 가능
        if (customUserDetails.getRole() != JwtTokenProvider.UserRole.BIF) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("BIF 사용자만 접근할 수 있습니다."));
        }

        try {
            final StatsResponse statsResponse = statsService.getMonthlyStats(customUserDetails.getBifId());
            return ResponseEntity.ok(ApiResponse.success(statsResponse));
        } catch (Exception e) {
            log.error("월별 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("통계 조회 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/guardian")
    @Operation(summary = "보호자 통계 조회", description = "보호자가 연결된 BIF의 통계를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "보호자 통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = GuardianStatsResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<GuardianStatsResponse>> getGuardianStats(
            @AuthenticationPrincipal final UserDetails userDetails,
            @RequestParam final Long bifId) {

        // UserDetails를 CustomUserDetails로 캐스팅
        if (!(userDetails instanceof CustomUserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증 정보가 올바르지 않습니다."));
        }
        
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        // Guardian 사용자만 접근 가능
        if (customUserDetails.getRole() != JwtTokenProvider.UserRole.GUARDIAN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보호자만 접근할 수 있습니다."));
        }

        try {
            final GuardianStatsResponse guardianStatsResponse = statsService.getGuardianStats(bifId);
            return ResponseEntity.ok(ApiResponse.success(guardianStatsResponse));
        } catch (Exception e) {
            log.error("보호자 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("보호자 통계 조회 중 오류가 발생했습니다."));
        }
    }

    // ========== 테스트용 공개 엔드포인트 ==========
    @GetMapping("/test/stat")
    @Operation(summary = "[테스트] 월별 통계 조회", description = "인증 없이 월별 통계를 조회합니다. (테스트용)")
    public ResponseEntity<ApiResponse<StatsResponse>> getMonthlyStatsTest(
            @RequestParam(name = "bifId", required = false) final Long bifIdParam) {
        try {
            final Long bifId = bifIdParam != null ? bifIdParam : 1L;
            final StatsResponse statsResponse = statsService.getMonthlyStats(bifId);
            return ResponseEntity.ok(ApiResponse.success(statsResponse));
        } catch (Exception e) {
            log.error("[테스트] 월별 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("통계 조회 중 오류가 발생했습니다."));
        }
    }
}
