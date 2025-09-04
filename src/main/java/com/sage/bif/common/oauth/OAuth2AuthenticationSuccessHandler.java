package com.sage.bif.common.oauth;

import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.service.SocialLoginService;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import com.sage.bif.user.service.LoginLogService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final int TEMP_TOKEN_MAX_AGE = 10 * 60;
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;
    private static final String TEMP_REGISTRATION_TOKEN_NAME = "tempRegistrationToken";
    private static final String AUTHENTICATED_USER_TOKEN_NAME = "authenticatedUserToken";

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final JwtTokenProvider jwtTokenProvider;
    private final SocialLoginService socialLoginService;
    private final BifService bifService;
    private final GuardianService guardianService;
    private final LoginLogService loginLogService;

    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider jwtTokenProvider, SocialLoginService socialLoginService,
                                              BifService bifService, GuardianService guardianService,
                                              LoginLogService loginLogService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.socialLoginService = socialLoginService;
        this.bifService = bifService;
        this.guardianService = guardianService;
        this.loginLogService = loginLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        clearAllExistingCookies(response);

        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = ((OAuth2User) authentication.getPrincipal()).getAttributes();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.get(registrationId, attributes);

        if (userInfo == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "사용자 정보를 가져올 수 없습니다.");
            return;
        }

        String email = userInfo.getEmail();
        String providerUniqueId = userInfo.getId();

        Optional<SocialLogin> existingSocialLogin = socialLoginService.findByProviderUniqueId(providerUniqueId);

        if (existingSocialLogin.isPresent()) {
            handleExistingUser(existingSocialLogin.get(), providerUniqueId, registrationId, response);
        } else {
            handleNewUser(email, providerUniqueId, registrationId, response);
        }
    }

    private void handleExistingUser(SocialLogin socialLogin, String providerUniqueId,
                                    String registrationId,
                                    HttpServletResponse response) throws IOException {

        Optional<com.sage.bif.user.entity.Bif> bif = bifService.findBySocialId(socialLogin.getSocialId());

        if (bif.isPresent()) {
            processBifLogin(bif.get().getBifId(), bif.get().getNickname(),
                    providerUniqueId, registrationId,
                    socialLogin.getSocialId(), response);
        } else {
            Optional<com.sage.bif.user.entity.Guardian> guardian = guardianService.findBySocialId(socialLogin.getSocialId());

            if (guardian.isPresent()) {
                processGuardianLogin(guardian.get().getBif().getBifId(), guardian.get().getNickname(),
                        providerUniqueId, registrationId,
                        socialLogin.getSocialId(), response);
            } else {
                handleIncompleteRegistration(socialLogin, providerUniqueId, registrationId, response);
            }
        }
    }

    private void processBifLogin(Long bifId, String nickname, String providerUniqueId,
                                 String registrationId, Long socialId,
                                 HttpServletResponse response) throws IOException {

        loginLogService.recordLogin(socialId);

        var socialLoginOpt = socialLoginService.findBySocialId(socialId);
        if (socialLoginOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "소셜 로그인 정보를 찾을 수 없습니다.");
            return;
        }

        var socialLogin = socialLoginOpt.get();

        String authenticatedUserToken = jwtTokenProvider.generateAuthenticatedUserToken(
                socialId, socialLogin.getEmail(), registrationId, providerUniqueId,
                "BIF", bifId, nickname
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(providerUniqueId);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(7);
        socialLoginService.saveRefreshToken(socialId, refreshToken, refreshTokenExpiresAt);

        setSecureCookie(response, AUTHENTICATED_USER_TOKEN_NAME, authenticatedUserToken, TEMP_TOKEN_MAX_AGE);
        setRefreshTokenCookie(response, refreshToken);

        response.sendRedirect(frontendUrl + "/");
    }

    private void processGuardianLogin(Long bifId, String nickname, String providerUniqueId,
                                      String registrationId, Long socialId,
                                      HttpServletResponse response) throws IOException {

        loginLogService.recordLogin(socialId);

        var socialLoginOpt = socialLoginService.findBySocialId(socialId);
        if (socialLoginOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "소셜 로그인 정보를 찾을 수 없습니다.");
            return;
        }

        var socialLogin = socialLoginOpt.get();

        String authenticatedUserToken = jwtTokenProvider.generateAuthenticatedUserToken(
                socialId, socialLogin.getEmail(), registrationId, providerUniqueId,
                "GUARDIAN", bifId, nickname
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(providerUniqueId);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(7);
        socialLoginService.saveRefreshToken(socialId, refreshToken, refreshTokenExpiresAt);

        setSecureCookie(response, AUTHENTICATED_USER_TOKEN_NAME, authenticatedUserToken, TEMP_TOKEN_MAX_AGE);
        setRefreshTokenCookie(response, refreshToken);

        response.sendRedirect(frontendUrl + "/");
    }

    private void handleIncompleteRegistration(SocialLogin socialLogin, String providerUniqueId,
                                              String registrationId,
                                              HttpServletResponse response) throws IOException {

        String tempRegistrationToken = jwtTokenProvider.generateTempRegistrationToken(
                socialLogin.getSocialId(),
                socialLogin.getEmail(),
                registrationId,
                providerUniqueId
        );

        setSecureCookie(response, TEMP_REGISTRATION_TOKEN_NAME, tempRegistrationToken, TEMP_TOKEN_MAX_AGE);
        response.sendRedirect(frontendUrl + "/login/select-role");
    }

    private void handleNewUser(String email, String providerUniqueId,
                               String registrationId,
                               HttpServletResponse response) throws IOException {

        SocialLogin socialLogin = socialLoginService.createSocialLogin(email,
                SocialLogin.SocialProvider.valueOf(registrationId.toUpperCase()), providerUniqueId);

        String tempRegistrationToken = jwtTokenProvider.generateTempRegistrationToken(
                socialLogin.getSocialId(),
                email,
                registrationId,
                providerUniqueId
        );

        setSecureCookie(response, TEMP_REGISTRATION_TOKEN_NAME, tempRegistrationToken, TEMP_TOKEN_MAX_AGE);
        response.sendRedirect(frontendUrl + "/login/select-role");
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        setSecureCookie(response, "refreshToken", refreshToken, REFRESH_TOKEN_MAX_AGE);
    }

    private void setSecureCookie(HttpServletResponse response, String name, String value, int maxAge) {
        String cookieValue = String.format(
                "%s=%s; Path=/; HttpOnly; SameSite=Lax; Max-Age=%d",
                name, value, maxAge
        );
        response.addHeader("Set-Cookie", cookieValue);

        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Origin", frontendUrl);
    }

    private void clearAllExistingCookies(HttpServletResponse response) {
        String[] cookiesToClear = { "accessToken", "refreshToken", AUTHENTICATED_USER_TOKEN_NAME };
        for (String cookieName : cookiesToClear) {
            Cookie cookie = new Cookie(cookieName, null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
    }

}
