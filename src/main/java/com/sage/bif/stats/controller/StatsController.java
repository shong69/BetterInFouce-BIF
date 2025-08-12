package com.sage.bif.stats.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.stats.dto.response.GuardianStatsResponse;
import com.sage.bif.stats.dto.response.StatsResponse;
import com.sage.bif.stats.service.StatsService;
import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.entity.Guardian;
import com.sage.bif.user.repository.GuardianRepository;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
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
    private final GuardianRepository guardianRepository;

    @GetMapping("/stats")
    @Operation(summary = "월별 통계 조회", description = "BIF 사용자의 월별 감정 통계를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = StatsResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<StatsResponse>> getMonthlyStats(
            @AuthenticationPrincipal final UserDetails userDetails) {

        if (!(userDetails instanceof CustomUserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증 정보가 올바르지 않습니다."));
        }
        
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

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

    @GetMapping("/bif_stats")
    @Operation(summary = "보호자 통계 조회", description = "보호자가 연결된 BIF의 통계를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "보호자 통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = GuardianStatsResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "연결된 BIF를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<GuardianStatsResponse>> getGuardianStats(
            @AuthenticationPrincipal final UserDetails userDetails,
            @RequestParam final Long bifId) {

        if (!(userDetails instanceof CustomUserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증 정보가 올바르지 않습니다."));
        }
        
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        if (customUserDetails.getRole() != JwtTokenProvider.UserRole.GUARDIAN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보호자만 접근할 수 있습니다."));
        }

        try {
            // 가디언이 해당 BIF와 연결되어 있는지 검증
            Guardian guardian = guardianRepository.findBySocialLogin_SocialId(customUserDetails.getSocialId())
                    .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND, "가디언 정보를 찾을 수 없습니다."));
            
            if (!guardian.getBif().getBifId().equals(bifId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("연결되지 않은 BIF의 통계는 조회할 수 없습니다."));
            }

            final GuardianStatsResponse guardianStatsResponse = statsService.getGuardianStats(bifId);
            return ResponseEntity.ok(ApiResponse.success(guardianStatsResponse));
        } catch (BaseException e) {
            log.error("보호자 통계 조회 중 비즈니스 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("보호자 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("보호자 통계 조회 중 오류가 발생했습니다."));
        }
    }

}
