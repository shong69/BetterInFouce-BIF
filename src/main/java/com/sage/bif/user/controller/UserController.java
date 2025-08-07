package com.sage.bif.user.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.dto.request.SocialRegistrationRequest;
import com.sage.bif.user.dto.response.BifResponse;
import com.sage.bif.user.dto.response.GuardianResponse;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import com.sage.bif.user.service.SocialLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

    private static final String PROVIDER_UNIQUE_ID = "providerUniqueId";
    private static final String PROVIDER = "provider";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String ACCESS_TOKEN = "accessToken";

    private final BifService bifService;
    private final GuardianService guardianService;
    private final SocialLoginService socialLoginService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register/bif")
    @Operation(summary = "소셜 로그인으로 BIF 회원가입", description = "소셜 로그인 정보를 사용하여 BIF 회원을 등록합니다. 닉네임과 연결 코드는 자동 생성됩니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerBifBySocial(
            @Valid @RequestBody SocialRegistrationRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        try {
            var bif = bifService.registerBySocialId(request.getSocialId(), request.getEmail());
            BifResponse response = BifResponse.from(bif);

            var socialLoginOpt = socialLoginService.findBySocialId(request.getSocialId());
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("소셜 로그인 정보를 찾을 수 없습니다."));
            }
            var socialLogin = socialLoginOpt.get();

            String accessToken = jwtTokenProvider.generateAccessToken(
                    JwtTokenProvider.UserRole.BIF,
                    bif.getBifId(),
                    bif.getNickname(),
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId(),
                    socialLogin.getSocialId()
            );

            String refreshToken = jwtTokenProvider.generateRefreshToken(socialLogin.getProviderUniqueId());
            LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(30);
            socialLoginService.saveRefreshToken(request.getSocialId(), refreshToken, refreshTokenExpiresAt);

            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN, refreshToken);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
            httpResponse.addCookie(refreshTokenCookie);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put(ACCESS_TOKEN, accessToken);
            responseData.put("bif", response);

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
            @Valid @RequestBody SocialRegistrationRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        try {
            var guardian = guardianService.registerBySocialId(
                    request.getSocialId(),
                    request.getEmail(),
                    request.getConnectionCode()
            );
            GuardianResponse response = GuardianResponse.from(guardian);

            var socialLoginOpt = socialLoginService.findBySocialId(request.getSocialId());
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("소셜 로그인 정보를 찾을 수 없습니다."));
            }
            var socialLogin = socialLoginOpt.get();

            String accessToken = jwtTokenProvider.generateAccessToken(
                    JwtTokenProvider.UserRole.GUARDIAN,
                    guardian.getBif().getBifId(),
                    guardian.getNickname(),
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId(),
                    socialLogin.getSocialId()
            );

            String refreshToken = jwtTokenProvider.generateRefreshToken(socialLogin.getProviderUniqueId());
            LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(30);
            socialLoginService.saveRefreshToken(request.getSocialId(), refreshToken, refreshTokenExpiresAt);

            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN, refreshToken);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
            httpResponse.addCookie(refreshTokenCookie);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put(ACCESS_TOKEN, accessToken);
            responseData.put("guardian", response);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(responseData, "보호자 회원가입 성공"));

        } catch (Exception e) {
            log.error("보호자 회원가입 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("보호자 회원가입 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "모든 인증 정보를 삭제하고 로그아웃합니다.")
    public ResponseEntity<ApiResponse<String>> logout(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response) {

        try {
            if (refreshToken != null && !refreshToken.isEmpty()) {
                String providerUniqueId = jwtTokenProvider.getProviderUniqueIdFromToken(refreshToken);
                var socialLoginOpt = socialLoginService.findByProviderUniqueId(providerUniqueId);

                if (socialLoginOpt.isPresent()) {
                    socialLoginService.deleteRefreshTokenFromRedis(socialLoginOpt.get().getSocialId());
                }
            }

            clearRefreshTokenCookie(response);

            return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));

        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("로그아웃 실패: " + e.getMessage()));
        }

    }

    @PostMapping("/refresh")
    @Operation(summary = "Access Token 갱신", description = "Refresh Token만으로 새로운 Access Token을 발급받습니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {

        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh Token이 없습니다."));
            }

            String providerUniqueId = jwtTokenProvider.getProviderUniqueIdFromToken(refreshToken);

            var socialLoginOpt = socialLoginService.findByProviderUniqueId(providerUniqueId);
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("사용자 정보를 찾을 수 없습니다."));
            }

            Long socialId = socialLoginOpt.get().getSocialId();

            if (!socialLoginService.validateRefreshToken(socialId, refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh Token이 유효하지 않습니다."));
            }

            Map<String, Object> responseData = generateNewTokens(socialLoginOpt.get(), response);

            return ResponseEntity.ok(ApiResponse.success(responseData, "Access Token 갱신 성공"));

        } catch (Exception e) {
            log.error("Access Token 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Access Token 갱신 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/session-info")
    @Operation(summary = "세션 정보 조회", description = "현재 세션의 사용자 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionInfo(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("세션이 없습니다."));
            }

            Map<String, Object> sessionData = new HashMap<>();

            String accessToken = (String) session.getAttribute(ACCESS_TOKEN);
            if (accessToken != null) {
                sessionData.put(ACCESS_TOKEN, accessToken);
                sessionData.put(PROVIDER_UNIQUE_ID, session.getAttribute(PROVIDER_UNIQUE_ID));
                sessionData.put("userRole", session.getAttribute("userRole"));
                sessionData.put("bifId", session.getAttribute("bifId"));
                sessionData.put("nickname", session.getAttribute("nickname"));
                sessionData.put(PROVIDER, session.getAttribute(PROVIDER));
            } else {
                Long registrationSocialId = (Long) session.getAttribute("registration_socialId");
                if (registrationSocialId != null) {
                    Map<String, Object> registrationInfo = new HashMap<>();
                    registrationInfo.put("socialId", registrationSocialId);
                    registrationInfo.put("email", session.getAttribute("registration_email"));
                    registrationInfo.put(PROVIDER, session.getAttribute("registration_provider"));
                    registrationInfo.put(PROVIDER_UNIQUE_ID, session.getAttribute("registration_providerUniqueId"));

                    sessionData.put("registrationInfo", registrationInfo);
                }
            }

            if (sessionData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("세션 정보가 없습니다."));
            }

            return ResponseEntity.ok(ApiResponse.success(sessionData, "세션 정보 조회 성공"));

        } catch (Exception e) {
            log.error("세션 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("세션 정보 조회 실패: " + e.getMessage()));
        }
    }

    private Map<String, Object> generateNewTokens(com.sage.bif.user.entity.SocialLogin socialLogin,
                                                  HttpServletResponse response) {

        var bif = bifService.findBySocialId(socialLogin.getSocialId());
        if (bif.isPresent()) {
            return generateBifTokens(bif.get(), socialLogin, response);
        }

        var guardian = guardianService.findBySocialId(socialLogin.getSocialId());
        if (guardian.isPresent()) {
            return generateGuardianTokens(guardian.get(), socialLogin, response);
        }

        throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    private Map<String, Object> generateBifTokens(com.sage.bif.user.entity.Bif bif,
                                                  com.sage.bif.user.entity.SocialLogin socialLogin,
                                                  HttpServletResponse response) {

        String newJwtToken = jwtTokenProvider.generateAccessToken(
                JwtTokenProvider.UserRole.BIF, bif.getBifId(), bif.getNickname(),
                socialLogin.getProvider().name(), socialLogin.getProviderUniqueId(),
                socialLogin.getSocialId()
        );

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(socialLogin.getProviderUniqueId());
        socialLoginService.saveRefreshToken(socialLogin.getSocialId(), newRefreshToken,
                LocalDateTime.now().plusDays(30));

        setRefreshTokenCookie(response, newRefreshToken);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("jwtToken", newJwtToken);

        return responseData;
    }

    private Map<String, Object> generateGuardianTokens(com.sage.bif.user.entity.Guardian guardian,
                                                       com.sage.bif.user.entity.SocialLogin socialLogin,
                                                       HttpServletResponse response) {

        String newJwtToken = jwtTokenProvider.generateAccessToken(
                JwtTokenProvider.UserRole.GUARDIAN, guardian.getBif().getBifId(), guardian.getNickname(),
                socialLogin.getProvider().name(), socialLogin.getProviderUniqueId(),
                socialLogin.getSocialId()
        );

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(socialLogin.getProviderUniqueId());
        socialLoginService.saveRefreshToken(socialLogin.getSocialId(), newRefreshToken,
                LocalDateTime.now().plusDays(30));

        setRefreshTokenCookie(response, newRefreshToken);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("jwtToken", newJwtToken);

        return responseData;
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN, refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN, "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
    }
}
