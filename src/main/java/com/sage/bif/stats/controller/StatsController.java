package com.sage.bif.stats.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.stats.dto.GuardianStatsResponse;
import com.sage.bif.stats.dto.StatsResponse;
import com.sage.bif.stats.service.StatsService;
import com.sage.bif.stats.service.StatsServiceImpl;
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
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.sage.bif.stats.entity.Stats;
import com.sage.bif.stats.repository.StatsRepository;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.diary.repository.DiaryRepository;

@Slf4j
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "통계 관련 API")
public class StatsController {

    private final StatsService statsService;
    private final GuardianRepository guardianRepository;
    private final StatsRepository statsRepository;
    private final DiaryRepository diaryRepository;

    private static final String ERROR_AUTH_INVALID = "인증 정보가 올바르지 않습니다.";
    private static final String ERROR_BIF_ONLY = "BIF 사용자만 접근할 수 있습니다.";
    private static final String ERROR_GUARDIAN_ONLY = "보호자만 접근할 수 있습니다.";


    private CustomUserDetails validateAuthentication(UserDetails userDetails) {
        if (!(userDetails instanceof CustomUserDetails customUserDetails)) {
            throw new IllegalArgumentException(ERROR_AUTH_INVALID);
        }
        return customUserDetails;
    }

    private void validateBifRole(CustomUserDetails userDetails) {
        if (userDetails.getRole() != JwtTokenProvider.UserRole.BIF) {
            throw new IllegalArgumentException(ERROR_BIF_ONLY);
        }
    }

    private void validateGuardianRole(CustomUserDetails userDetails) {
        if (userDetails.getRole() != JwtTokenProvider.UserRole.GUARDIAN) {
            throw new IllegalArgumentException(ERROR_GUARDIAN_ONLY);
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "월별 통계 조회", description = "BIF 사용자의 월별 감정 통계를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 조회 성공",
                    content = @Content(schema = @Schema(implementation = StatsResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<StatsResponse>> getMonthlyStats(
            @AuthenticationPrincipal final UserDetails userDetails,
            @RequestParam(required = false) final Long bifId) {

        try {
            final CustomUserDetails customUserDetails = validateAuthentication(userDetails);
            validateBifRole(customUserDetails);

            final Long targetBifId = bifId != null ? bifId : customUserDetails.getBifId();
            final StatsResponse statsResponse = statsService.getMonthlyStats(targetBifId);
            return ResponseEntity.ok(ApiResponse.success(statsResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
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

        try {
            final CustomUserDetails customUserDetails = validateAuthentication(userDetails);
            validateGuardianRole(customUserDetails);

            Guardian guardian = guardianRepository.findBySocialLogin_SocialId(customUserDetails.getSocialId())
                    .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND, "가디언 정보를 찾을 수 없습니다."));

            if (!guardian.getBif().getBifId().equals(bifId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("연결되지 않은 BIF의 통계는 조회할 수 없습니다."));
            }

            final GuardianStatsResponse base = statsService.getGuardianStats(bifId);

            final String joinDate = guardian.getCreatedAt() != null
                    ? guardian.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "";

            final GuardianStatsResponse response = GuardianStatsResponse.builder()
                    .bifNickname(base.getBifNickname())
                    .advice(base.getAdvice())
                    .guardianJoinDate(joinDate)
                    .emotionRatio(base.getEmotionRatio())
                    .monthlyChange(base.getMonthlyChange())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
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

    @PostMapping("/force-regenerate/{bifId}")
    @Operation(summary = "통계 데이터 강제 재생성", description = "BIF 사용자의 통계 데이터를 강제로 재생성하고 캐시를 무효화합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 재생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<String>> forceRegenerateStats(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long bifId) {

        try {
            final CustomUserDetails customUserDetails = validateAuthentication(userDetails);
            validateBifRole(customUserDetails);

            statsService.forceRegenerateStats(bifId);
            return ResponseEntity.ok(ApiResponse.success("통계 데이터가 성공적으로 재생성되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("통계 데이터 강제 재생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("통계 재생성 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/cleanup-keywords/{bifId}")
    @Operation(summary = "잘못된 키워드 정리", description = "BIF 사용자의 통계에서 잘못된 키워드들을 정리합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "키워드 정리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<String>> cleanupInvalidKeywords(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long bifId) {

        try {
            final CustomUserDetails customUserDetails = validateAuthentication(userDetails);
            validateBifRole(customUserDetails);

            ((StatsServiceImpl) statsService).cleanupInvalidKeywords(bifId);
            return ResponseEntity.ok(ApiResponse.success("잘못된 키워드가 성공적으로 정리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("키워드 정리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("키워드 정리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/reset-keywords/{bifId}")
    @Operation(summary = "키워드 데이터 초기화", description = "BIF 사용자의 키워드 데이터를 초기화합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "키워드 초기화 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<String>> resetKeywords(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long bifId) {

        try {
            final CustomUserDetails customUserDetails = validateAuthentication(userDetails);
            validateBifRole(customUserDetails);

            statsService.resetKeywords(bifId);
            return ResponseEntity.ok(ApiResponse.success("키워드 데이터가 성공적으로 초기화되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("키워드 초기화 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("키워드 초기화 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/debug-keywords/{bifId}")
    @Operation(summary = "키워드 디버깅", description = "BIF 사용자의 키워드 관련 디버깅 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "디버깅 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugKeywords(
            @AuthenticationPrincipal final UserDetails userDetails,
            @PathVariable final Long bifId) {

        try {
            final CustomUserDetails customUserDetails = validateAuthentication(userDetails);
            validateBifRole(customUserDetails);

            log.info("=== 키워드 디버깅 시작 - BIF ID: {} ===", bifId);
            
            final LocalDateTime currentYearMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            final Optional<Stats> stats = statsRepository.findFirstByBifIdAndYearMonthOrderByCreatedAtDesc(bifId, currentYearMonth);
            
            Map<String, Object> debugInfo = new HashMap<>();
            
            if (stats.isPresent()) {
                debugInfo.put("statsExists", true);
                debugInfo.put("topKeywords", stats.get().getTopKeywords());
                debugInfo.put("emotionStatisticsText", stats.get().getEmotionStatisticsText());
                debugInfo.put("guardianAdviceText", stats.get().getGuardianAdviceText());
                debugInfo.put("createdAt", stats.get().getCreatedAt());
                debugInfo.put("updatedAt", stats.get().getUpdatedAt());
            } else {
                debugInfo.put("statsExists", false);
            }
            
            final List<Diary> monthlyDiaries = diaryRepository.findByUserIdAndDateBetween(
                bifId, 
                currentYearMonth, 
                currentYearMonth.plusMonths(1).minusSeconds(1)
            );
            debugInfo.put("monthlyDiariesCount", monthlyDiaries.size());
            
            if (!monthlyDiaries.isEmpty()) {
                List<Map<String, Object>> diaryInfo = new ArrayList<>();
                for (Diary diary : monthlyDiaries) {
                    Map<String, Object> diaryData = new HashMap<>();
                    diaryData.put("id", diary.getId());
                    diaryData.put("contentLength", diary.getContent() != null ? diary.getContent().length() : 0);
                    diaryData.put("contentPreview", diary.getContent() != null ? 
                        diary.getContent().substring(0, Math.min(100, diary.getContent().length())) : "null");
                    diaryData.put("createdAt", diary.getCreatedAt());
                    diaryData.put("isDeleted", diary.isDeleted());
                    diaryInfo.add(diaryData);
                }
                debugInfo.put("diaries", diaryInfo);
            }
            
            log.info("=== 키워드 디버깅 완료 - BIF ID: {} ===", bifId);
            
            return ResponseEntity.ok(ApiResponse.success(debugInfo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("키워드 디버깅 중 오류 발생 - bifId: {}", bifId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("키워드 디버깅 중 오류가 발생했습니다."));
        }
    }

}
