package com.sage.bif.user.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.dto.request.SocialRegistrationRequest;
import com.sage.bif.user.dto.response.BifResponse;
import com.sage.bif.user.dto.response.GuardianResponse;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import com.sage.bif.user.service.SocialLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "User Management", description = "사용자 관리 API")
@RequiredArgsConstructor
public class UserController {

    // 상수 정의
    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final String REFRESH_TOKEN_KEY = "refreshToken";
    private static final String BIF_KEY = "bif";
    private static final String GUARDIAN_KEY = "guardian";

    private final BifService bifService;
    private final GuardianService guardianService;
    private final SocialLoginService socialLoginService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register/bif")
    @Operation(summary = "소셜 로그인으로 BIF 회원가입", description = "소셜 로그인 정보를 사용하여 BIF 회원을 등록합니다. 닉네임과 연결 코드는 자동 생성됩니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerBifBySocial(
            @Valid @RequestBody SocialRegistrationRequest request) {

        try {
            var bif = bifService.registerBySocialId(request.getSocialId(), request.getEmail());
            BifResponse response = BifResponse.from(bif);

            // 소셜 로그인 정보 조회 (isPresent 체크 추가)
            var socialLoginOpt = socialLoginService.findBySocialId(request.getSocialId());
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("소셜 로그인 정보를 찾을 수 없습니다."));
            }
            var socialLogin = socialLoginOpt.get();

            // Access Token 생성 (BIF 역할)
            String accessToken = jwtTokenProvider.generateAccessToken(
                    JwtTokenProvider.UserRole.BIF,
                    bif.getBifId(),
                    bif.getNickname(),
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId()
            );

            // Refresh Token 생성 및 저장
            String refreshToken = jwtTokenProvider.generateRefreshToken(request.getEmail());
            LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(30);
            socialLoginService.saveRefreshToken(request.getSocialId(), refreshToken, refreshTokenExpiresAt);

            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put(BIF_KEY, response);
            responseData.put(ACCESS_TOKEN_KEY, accessToken);
            responseData.put(REFRESH_TOKEN_KEY, refreshToken);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(responseData, "BIF 회원가입 성공"));

        } catch (Exception e) {
            log.error("BIF 회원가입 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("BIF 회원가입 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/register/guardian")
    @Operation(summary = "소셜 로그인으로 보호자 회원가입", description = "소셜 로그인 정보와 연결 코드를 사용하여 보호자를 등록합니다. 닉네임은 자동 생성됩니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerGuardianBySocial(
            @Valid @RequestBody SocialRegistrationRequest request) {

        try {
            var guardian = guardianService.registerBySocialId(
                    request.getSocialId(),
                    request.getEmail(),
                    request.getConnectionCode()
            );
            GuardianResponse response = GuardianResponse.from(guardian);

            // 소셜 로그인 정보 조회 (isPresent 체크 추가)
            var socialLoginOpt = socialLoginService.findBySocialId(request.getSocialId());
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("소셜 로그인 정보를 찾을 수 없습니다."));
            }
            var socialLogin = socialLoginOpt.get();

            // Access Token 생성 (GUARDIAN 역할)
            String accessToken = jwtTokenProvider.generateAccessToken(
                    JwtTokenProvider.UserRole.GUARDIAN,
                    guardian.getBif().getBifId(), // 연결된 BIF ID
                    guardian.getNickname(),
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId()
            );

            // Refresh Token 생성 및 저장
            String refreshToken = jwtTokenProvider.generateRefreshToken(request.getEmail());
            LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(30);
            socialLoginService.saveRefreshToken(request.getSocialId(), refreshToken, refreshTokenExpiresAt);

            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put(GUARDIAN_KEY, response);
            responseData.put(ACCESS_TOKEN_KEY, accessToken);
            responseData.put(REFRESH_TOKEN_KEY, refreshToken);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(responseData, "보호자 회원가입 성공"));

        } catch (Exception e) {
            log.error("보호자 회원가입 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("보호자 회원가입 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access Token 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @RequestParam Long socialId,
            @RequestParam String refreshToken) {

        try {
            // Refresh Token 유효성 검증
            if (!socialLoginService.validateRefreshToken(socialId, refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("유효하지 않은 Refresh Token입니다."));
            }

            // 소셜 로그인 정보 조회 (isPresent 체크 추가)
            var socialLoginOpt = socialLoginService.findBySocialId(socialId);
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("사용자 정보를 찾을 수 없습니다."));
            }
            var socialLogin = socialLoginOpt.get();

            // BIF/Guardian 정보 조회하여 새로운 Access Token 생성
            var bif = bifService.findBySocialId(socialId);
            if (bif.isPresent()) {
                // BIF 회원
                String newAccessToken = jwtTokenProvider.generateAccessToken(
                        JwtTokenProvider.UserRole.BIF,
                        bif.get().getBifId(),
                        bif.get().getNickname(),
                        socialLogin.getProvider().name(),
                        socialLogin.getProviderUniqueId()
                );

                Map<String, Object> responseData = new HashMap<>();
                responseData.put(ACCESS_TOKEN_KEY, newAccessToken);
                responseData.put(REFRESH_TOKEN_KEY, refreshToken);

                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(responseData, "Access Token 갱신 성공"));
            } else {
                // Guardian 회원
                var guardian = guardianService.findBySocialId(socialId);
                if (guardian.isPresent()) {
                    String newAccessToken = jwtTokenProvider.generateAccessToken(
                            JwtTokenProvider.UserRole.GUARDIAN,
                            guardian.get().getBif().getBifId(),
                            guardian.get().getNickname(),
                            socialLogin.getProvider().name(),
                            socialLogin.getProviderUniqueId()
                    );

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put(ACCESS_TOKEN_KEY, newAccessToken);
                    responseData.put(REFRESH_TOKEN_KEY, refreshToken);

                    return ResponseEntity.status(HttpStatus.OK)
                            .body(ApiResponse.success(responseData, "Access Token 갱신 성공"));
                }
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("사용자 정보를 찾을 수 없습니다."));

        } catch (Exception e) {
            log.error("Access Token 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Access Token 갱신 실패: " + e.getMessage()));
        }
    }
}
