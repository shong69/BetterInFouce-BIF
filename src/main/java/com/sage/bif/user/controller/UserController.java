package com.sage.bif.user.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.dto.request.SocialRegistrationRequest;
import com.sage.bif.user.dto.response.BifResponse;
import com.sage.bif.user.dto.response.GuardianResponse;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import com.sage.bif.user.service.LoginLogService;
import com.sage.bif.user.service.SocialLoginService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private static final String PROVIDER_UNIQUE_ID = "providerUniqueId";
    private static final String PROVIDER = "provider";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String ROLE = "userRole";
    private static final String BIF_ID = "bifId";
    private static final String NICKNAME = "nickname";

    private final BifService bifService;
    private final GuardianService guardianService;
    private final SocialLoginService socialLoginService;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginLogService loginLogService;

    @PostMapping("/register/bif")
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

            loginLogService.recordLogin(request.getSocialId());

            String accessToken = jwtTokenProvider.generateAccessToken(
                    JwtTokenProvider.UserRole.BIF,
                    bif.getBifId(),
                    bif.getNickname(),
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId(),
                    socialLogin.getSocialId()
            );

            String refreshToken = jwtTokenProvider.generateRefreshToken(socialLogin.getProviderUniqueId());
            LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(7);
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

            loginLogService.recordLogin(request.getSocialId());

            String accessToken = jwtTokenProvider.generateAccessToken(
                    JwtTokenProvider.UserRole.GUARDIAN,
                    guardian.getBif().getBifId(),
                    guardian.getNickname(),
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId(),
                    socialLogin.getSocialId()
            );

            String refreshToken = jwtTokenProvider.generateRefreshToken(socialLogin.getProviderUniqueId());
            LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(7);
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
    public ResponseEntity<ApiResponse<String>> logout(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            if (refreshToken != null && !refreshToken.isEmpty()) {
                String providerUniqueId = jwtTokenProvider.getProviderUniqueIdFromToken(refreshToken);
                var socialLoginOpt = socialLoginService.findByProviderUniqueId(providerUniqueId);

                if (socialLoginOpt.isPresent()) {
                    Long socialId = socialLoginOpt.get().getSocialId();
                    loginLogService.recordLogout(socialId);
                    socialLoginService.deleteRefreshTokenFromRedis(socialLoginOpt.get().getSocialId());
                }
            }

            clearRefreshTokenCookie(response);

            HttpSession session = request.getSession(false);
            if(session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));

        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("로그아웃 실패: " + e.getMessage()));
        }

    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {

        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh Token이 없습니다.", "MISSING_REFRESH_TOKEN"));
            }

            String validationResult = jwtTokenProvider.validateToken(refreshToken);
            if (!"SUCCESS".equals(validationResult)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh Token이 유효하지 않습니다.", validationResult));
            }

            String providerUniqueId = jwtTokenProvider.getProviderUniqueIdFromToken(refreshToken);

            if (providerUniqueId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("토큰에서 사용자 정보를 추출할 수 없습니다.", "INVALID_TOKEN_FORMAT"));
            }


            var socialLoginOpt = socialLoginService.findByProviderUniqueId(providerUniqueId);
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("사용자 정보를 찾을 수 없습니다.", "USER_NOT_FOUND"));
            }

            Long socialId = socialLoginOpt.get().getSocialId();

            if (!socialLoginService.validateRefreshToken(socialId, refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh Token이 유효하지 않습니다.", "REDIS_TOKEN_MISMATCH"));
            }

            var bif = bifService.findBySocialId(socialId);
            var guardian = guardianService.findBySocialId(socialId);

            if (bif.isEmpty() && guardian.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("등록되지 않은 사용자입니다.", "UNREGISTERED_USER"));
            }

            Map<String, Object> responseData = generateNewTokens(socialLoginOpt.get(), response);

            return ResponseEntity.ok(ApiResponse.success(responseData, "Access Token 갱신 성공"));

        } catch (Exception e) {
            log.error("Access Token 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", "SERVER_ERROR"));
        }
    }

    @GetMapping("/session-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionInfo(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpSession session = request.getSession(false);
            Map<String, Object> sessionData = new HashMap<>();

            if (session != null) {
                String accessToken = (String) session.getAttribute(ACCESS_TOKEN);
                if (accessToken != null) {
                    // 로그인된 사용자
                    sessionData.put(ACCESS_TOKEN, accessToken);
                    sessionData.put(PROVIDER_UNIQUE_ID, session.getAttribute(PROVIDER_UNIQUE_ID));
                    sessionData.put(ROLE, session.getAttribute(ROLE));
                    sessionData.put(BIF_ID, session.getAttribute(BIF_ID));
                    sessionData.put(NICKNAME, session.getAttribute(NICKNAME));
                    sessionData.put(PROVIDER, session.getAttribute(PROVIDER));

                    return ResponseEntity.ok(ApiResponse.success(sessionData, "세션 정보 조회 성공"));
                }
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof OAuth2AuthenticationToken oauth2Token) {
                oauth2Token = (OAuth2AuthenticationToken) auth;
                String providerUniqueId = oauth2Token.getPrincipal().getName();

                var socialLoginOpt = socialLoginService.findByProviderUniqueId(providerUniqueId);
                if (socialLoginOpt.isPresent()) {
                    Long socialId = socialLoginOpt.get().getSocialId();

                    var bif = bifService.findBySocialId(socialId);
                    var guardian = guardianService.findBySocialId(socialId);

                    if (bif.isPresent() || guardian.isPresent()) {
                        Map<String, Object> tokenData = generateNewTokens(socialLoginOpt.get(), response);
                        sessionData.putAll(tokenData);

                        session.removeAttribute("registration_socialId");
                        session.removeAttribute("registration_email");
                        session.removeAttribute("registration_provider");
                        session.removeAttribute("registration_providerUniqueId");

                    } else {
                        String email = oauth2Token.getPrincipal().getAttribute("email");
                        String provider = oauth2Token.getAuthorizedClientRegistrationId();

                        Map<String, Object> registrationInfo = new HashMap<>();
                        registrationInfo.put("socialId", socialId);
                        registrationInfo.put("email", email);
                        registrationInfo.put(PROVIDER, provider);
                        registrationInfo.put(PROVIDER_UNIQUE_ID, providerUniqueId);

                        sessionData.put("registrationInfo", registrationInfo);
                    }
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

    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<String>> withdrawUser(
            Authentication authentication, HttpServletResponse response) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long socialId = userDetails.getSocialId();
            JwtTokenProvider.UserRole userRole = userDetails.getRole();

            if (userRole == JwtTokenProvider.UserRole.BIF) {
                bifService.deleteBySocialId(socialId);
            } else if (userRole == JwtTokenProvider.UserRole.GUARDIAN) {
                guardianService.deleteBySocialId(socialId);
            }

            socialLoginService.deleteBySocialId(socialId);

            socialLoginService.deleteRefreshTokenFromRedis(socialId);

            clearRefreshTokenCookie(response);

            LocalDateTime withdrawalDate = LocalDateTime.now();
            loginLogService.scheduleLogsForDeletion(socialId, withdrawalDate);

            return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다."));

        } catch (Exception e) {
            log.error("회원탈퇴 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("회원탈퇴 중 오류가 발생했습니다.", "SERVER_ERROR"));
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
                LocalDateTime.now().plusDays(7));

        setRefreshTokenCookie(response, newRefreshToken);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put(ACCESS_TOKEN, newJwtToken);
        responseData.put(PROVIDER_UNIQUE_ID, socialLogin.getProviderUniqueId());
        responseData.put(ROLE, "BIF");
        responseData.put(BIF_ID, bif.getBifId());
        responseData.put(NICKNAME, bif.getNickname());
        responseData.put(PROVIDER, socialLogin.getProvider().name());

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
                LocalDateTime.now().plusDays(7));

        setRefreshTokenCookie(response, newRefreshToken);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put(ACCESS_TOKEN, newJwtToken);
        responseData.put(PROVIDER_UNIQUE_ID, socialLogin.getProviderUniqueId());
        responseData.put(ROLE, "GUARDIAN");
        responseData.put(BIF_ID, guardian.getBif().getBifId());
        responseData.put(NICKNAME, guardian.getNickname());
        responseData.put(PROVIDER, socialLogin.getProvider().name());

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
