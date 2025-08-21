package com.sage.bif.user.controller;

import com.sage.bif.common.dto.ApiResponse;
import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.dto.request.NicknameChangeRequest;
import com.sage.bif.user.dto.request.SocialRegistrationRequest;
import com.sage.bif.user.dto.response.BifResponse;
import com.sage.bif.user.dto.response.GuardianResponse;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import com.sage.bif.user.service.LoginLogService;
import com.sage.bif.user.service.SocialLoginService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.sage.bif.common.exception.ErrorCode.AUTH_FAILED;

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
    private static final String SERVER_ERROR = "SERVER_ERROR";
    private static final String TEMP_REGISTRATION_TOKEN = "tempRegistrationToken";
    private static final String AUTHENTICATED_USER_TOKEN = "authenticatedUserToken";
    private static final String SUCCESS = "SUCCESS";

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

            String authenticatedUserToken = jwtTokenProvider.generateAuthenticatedUserToken(
                    socialLogin.getSocialId(),
                    socialLogin.getEmail(),
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId(),
                    "BIF",
                    bif.getBifId(),
                    bif.getNickname()
            );

            setSecureCookie(httpResponse, AUTHENTICATED_USER_TOKEN, authenticatedUserToken, 30 * 60);
            setSecureCookie(httpResponse, REFRESH_TOKEN, refreshToken, REFRESH_TOKEN_MAX_AGE);
            setSecureCookie(httpResponse, TEMP_REGISTRATION_TOKEN, "", 0);

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

            String authenticatedUserToken = jwtTokenProvider.generateAuthenticatedUserToken(
                    socialLogin.getSocialId(),
                    socialLogin.getEmail(),
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId(),
                    "GUARDIAN",
                    guardian.getBif().getBifId(),
                    guardian.getNickname()
            );

            setSecureCookie(httpResponse, AUTHENTICATED_USER_TOKEN, authenticatedUserToken, 30 * 60);
            setSecureCookie(httpResponse, REFRESH_TOKEN, refreshToken, REFRESH_TOKEN_MAX_AGE);
            setSecureCookie(httpResponse, TEMP_REGISTRATION_TOKEN, "", 0);

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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response) {

        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
            }

            Long socialId = userDetails.getSocialId();
            log.info("socialId: "+socialId);

            loginLogService.recordLogout(socialId);

            socialLoginService.deleteRefreshTokenFromRedis(socialId);

            clearAllJwtCookies(response);

            return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));

        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            clearAllJwtCookies(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("로그아웃 실패: " + e.getMessage()));
        }

    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @CookieValue(value="refreshToken", required=false) String refreshToken, HttpServletResponse response) {

        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh Token이 없습니다.", "MISSING_REFRESH_TOKEN"));
            }

            String validationResult = jwtTokenProvider.validateToken(refreshToken);
            if (!SUCCESS.equals(validationResult)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("토큰 형식이 유효하지 않습니다.", validationResult));
            }

            String providerUniqueId = jwtTokenProvider.getProviderUniqueIdFromToken(refreshToken);

            if (providerUniqueId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("토큰에서 사용자 정보를 추출할 수 없습니다.", "AUTH_ACCOUNT_NOT_FOUND"));
            }

            var socialLoginOpt = socialLoginService.findByProviderUniqueId(providerUniqueId);
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("사용자 정보를 찾을 수 없습니다.", "USER_NOT_FOUND"));
            }

            Long socialId = socialLoginOpt.get().getSocialId();

            if (!socialLoginService.validateRefreshToken(socialId, refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Refresh Token이 유효하지 않습니다.", "AUTH_TOKEN_INVALID"));
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
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", SERVER_ERROR));
        }
    }

    @GetMapping("/session-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionInfo(
            HttpServletRequest request,
            @CookieValue(value = TEMP_REGISTRATION_TOKEN, required = false) String tempRegistrationToken,
            @CookieValue(value = AUTHENTICATED_USER_TOKEN, required = false) String authenticatedUserToken) {

        try {
            logCookieInformation(request, tempRegistrationToken, authenticatedUserToken);

            if (tempRegistrationToken != null) {
                var registrationResult = processRegistrationToken(tempRegistrationToken);
                if (registrationResult != null) {
                    return registrationResult;
                }
            }

            if (authenticatedUserToken != null) {
                var userResult = processAuthenticatedUserToken(authenticatedUserToken);
                if (userResult != null) {
                    return userResult;
                }
            }

            var oauth2Result = processOAuth2Authentication();
            if (oauth2Result != null) {
                return oauth2Result;
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증 정보가 없습니다."));

        } catch (Exception e) {
            log.error("세션 정보 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("세션 정보 조회 실패: " + e.getMessage()));
        }
    }

    private void logCookieInformation(HttpServletRequest request, String tempToken, String authToken) {
        log.info("Session-info called - tempRegistrationToken: {}, authenticatedUserToken: {}",
                tempToken != null ? "present" : "null",
                authToken != null ? "present" : "null");

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.debug("Cookie - Name: {}, Domain: {}, Path: {}",
                        cookie.getName(), cookie.getDomain(), cookie.getPath());
            }
        } else {
            log.warn("No cookies found in request");
        }
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> processRegistrationToken(String token) {
        log.info("Processing tempRegistrationToken...");
        String validationResult = jwtTokenProvider.validateToken(token);
        log.info("Token validation result: {}", validationResult);

        if (SUCCESS.equals(validationResult)) {
            Map<String, Object> registrationInfo = jwtTokenProvider.getRegistrationInfoFromTempToken(token);
            log.info("Registration info extracted: {}", registrationInfo != null ? "success" : "failed");

            if (registrationInfo != null) {
                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("registrationInfo", registrationInfo);
                return ResponseEntity.ok(ApiResponse.success(sessionData, "임시 등록 정보 조회 성공"));
            }
        } else {
            log.warn("Invalid temp registration token: {}", validationResult);
        }
        return null;
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> processAuthenticatedUserToken(String token) {
        log.info("Processing authenticatedUserToken...");
        String validationResult = jwtTokenProvider.validateToken(token);
        log.info("Auth token validation result: {}", validationResult);

        if (SUCCESS.equals(validationResult)) {
            Map<String, Object> userInfo = jwtTokenProvider.getAuthenticatedUserInfoFromToken(token);
            log.info("User info extracted: {}", userInfo != null ? "success" : "failed");

            if (userInfo != null) {
                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put(ACCESS_TOKEN, token);
                sessionData.put(PROVIDER_UNIQUE_ID, userInfo.get(PROVIDER_UNIQUE_ID));
                sessionData.put(ROLE, userInfo.get(ROLE));
                sessionData.put(BIF_ID, userInfo.get(BIF_ID));
                sessionData.put(NICKNAME, userInfo.get(NICKNAME));
                sessionData.put(PROVIDER, userInfo.get(PROVIDER));

                return ResponseEntity.ok(ApiResponse.success(sessionData, "인증된 사용자 정보 조회 성공"));
            }
        } else {
            log.warn("Invalid authenticated user token: {}", validationResult);
        }
        return null;
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> processOAuth2Authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof OAuth2AuthenticationToken oauth2Token)) {
            return null;
        }

        String providerUniqueId = oauth2Token.getPrincipal().getName();
        var socialLoginOpt = socialLoginService.findByProviderUniqueId(providerUniqueId);

        if (socialLoginOpt.isEmpty()) {
            return null;
        }

        Long socialId = socialLoginOpt.get().getSocialId();
        var bif = bifService.findBySocialId(socialId);
        var guardian = guardianService.findBySocialId(socialId);

        if (bif.isPresent() || guardian.isPresent()) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("기존 사용자입니다. 다시 로그인해주세요.", "EXISTING_USER_LOGOUT_REQUIRED"));
        }

        String email = socialLoginOpt.map(SocialLogin::getEmail).orElse(null);
        String provider = oauth2Token.getAuthorizedClientRegistrationId();

        Map<String, Object> registrationInfo = new HashMap<>();
        registrationInfo.put("socialId", socialId);
        registrationInfo.put("email", email);
        registrationInfo.put(PROVIDER, provider);
        registrationInfo.put(PROVIDER_UNIQUE_ID, providerUniqueId);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("registrationInfo", registrationInfo);

        return ResponseEntity.ok(ApiResponse.success(sessionData, "OAuth2 등록 정보 조회 성공"));
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<String>> withdrawUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
            }

            Long socialId = userDetails.getSocialId();
            JwtTokenProvider.UserRole userRole = userDetails.getRole();

            loginLogService.recordLogout(socialId);

            if (userRole == JwtTokenProvider.UserRole.BIF) {
                bifService.deleteBySocialId(socialId);
            } else if (userRole == JwtTokenProvider.UserRole.GUARDIAN) {
                guardianService.deleteBySocialId(socialId);
            }

            socialLoginService.deleteBySocialId(socialId);

            socialLoginService.deleteRefreshTokenFromRedis(socialId);

            clearAllJwtCookies(response);
            HttpSession session = request.getSession(false);
            if(session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();

            LocalDateTime withdrawalDate = LocalDateTime.now();
            loginLogService.scheduleLogsForDeletion(socialId, withdrawalDate);

            return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다."));

        } catch (Exception e) {
            log.error("회원탈퇴 실패: {}", e.getMessage());
            clearAllJwtCookies(response);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("회원탈퇴 중 오류가 발생했습니다.", SERVER_ERROR));
        }
    }

    @PostMapping("/changenickname")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeNickname(
            @Valid @RequestBody NicknameChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            Long socialId = userDetails.getSocialId();
            JwtTokenProvider.UserRole userRole = userDetails.getRole();
            String newNickname = request.getNickname();

            if (userRole == JwtTokenProvider.UserRole.BIF) {
                bifService.updateNickname(socialId, newNickname);
            } else if (userRole == JwtTokenProvider.UserRole.GUARDIAN) {
                guardianService.updateNickname(socialId, newNickname);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("유효하지 않은 사용자 역할입니다.", "INVALID_USER_ROLE"));
            }

            var socialLoginOpt = socialLoginService.findBySocialId(socialId);
            if (socialLoginOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("사용자 정보를 찾을 수 없습니다."));
            }

            var socialLogin = socialLoginOpt.get();
            Long bifId = userDetails.getBifId();

            String newAccessToken = jwtTokenProvider.generateAccessToken(
                    userRole, bifId, newNickname,
                    socialLogin.getProvider().name(),
                    socialLogin.getProviderUniqueId(),
                    socialId
            );

            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                session.setAttribute(ACCESS_TOKEN, newAccessToken);
                session.setAttribute(NICKNAME, newNickname);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put(ACCESS_TOKEN, newAccessToken);
            responseData.put(NICKNAME, newNickname);
            responseData.put("message", "닉네임이 성공적으로 변경되었습니다.");

            return ResponseEntity.ok(ApiResponse.success(responseData, "닉네임 변경 성공"));

        } catch (BaseException e) {
            log.error("닉네임 변경 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode().name()));
        } catch (Exception e) {
            log.error("닉네임 변경 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", SERVER_ERROR));
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
        setSecureCookie(response, REFRESH_TOKEN, refreshToken, REFRESH_TOKEN_MAX_AGE);
    }

    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;

    private void setSecureCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setMaxAge(maxAge);

        response.addCookie(cookie);

        String cookieHeader = String.format(
                "%s=%s; Path=/; HttpOnly; SameSite=Lax; Max-Age=%d",
                name, value, maxAge
        );
        response.addHeader("Set-Cookie", cookieHeader);
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private void clearAllJwtCookies(HttpServletResponse response) {
        String[] cookiesToClear = {
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                AUTHENTICATED_USER_TOKEN,
                TEMP_REGISTRATION_TOKEN
        };

        for (String cookieName : cookiesToClear ) {
            Cookie rootCookie = new Cookie(cookieName, null);
            rootCookie.setMaxAge(0);
            rootCookie.setPath("/");
            rootCookie.setHttpOnly(true);
            rootCookie.setSecure(false);
            response.addCookie(rootCookie);
        }
    }

}
