package com.sage.bif.common.oauth;

import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.service.SocialLoginService;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import com.sage.bif.user.service.LoginLogService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
            handleExistingUser(existingSocialLogin.get(), providerUniqueId, registrationId, request, response);
        } else {
            handleNewUser(email, providerUniqueId, registrationId, request, response);
        }
    }

    private void handleExistingUser(SocialLogin socialLogin, String providerUniqueId,
                                    String registrationId,
                                    HttpServletRequest request, HttpServletResponse response) throws IOException {

        Optional<com.sage.bif.user.entity.Bif> bif = bifService.findBySocialId(socialLogin.getSocialId());
        if (bif.isPresent()) {
            loginLogService.recordLogin(socialLogin.getSocialId());
            processBifLogin(bif.get().getBifId(), bif.get().getNickname(),
                    providerUniqueId, registrationId,
                    socialLogin.getSocialId(), request, response);
        } else {
            Optional<com.sage.bif.user.entity.Guardian> guardian = guardianService.findBySocialId(socialLogin.getSocialId());

            if (guardian.isPresent()) {
                loginLogService.recordLogin(socialLogin.getSocialId());
                processGuardianLogin(guardian.get().getBif().getBifId(), guardian.get().getNickname(),
                        providerUniqueId, registrationId,
                        socialLogin.getSocialId(), request, response);
            } else {
                handleIncompleteRegistration(socialLogin, providerUniqueId, registrationId, request, response);
            }
        }
    }

    private void processBifLogin(Long bifId, String nickname, String providerUniqueId,
                                 String registrationId, Long socialId,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {

        String accessToken = jwtTokenProvider.generateAccessToken(
                JwtTokenProvider.UserRole.BIF, bifId, nickname, registrationId, providerUniqueId, socialId
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(providerUniqueId);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(7);
        socialLoginService.saveRefreshToken(socialId, refreshToken, refreshTokenExpiresAt);

        setRefreshTokenCookie(response, refreshToken);

        saveUserSession(request, new CustomUserDetails(accessToken, bifId, nickname, registrationId, providerUniqueId, JwtTokenProvider.UserRole.BIF, socialId));

        response.sendRedirect(frontendUrl + "/");
    }


    private void processGuardianLogin(Long bifId, String nickname, String providerUniqueId,
                                      String registrationId, Long socialId,
                                      HttpServletRequest request, HttpServletResponse response) throws IOException {

        String accessToken = jwtTokenProvider.generateAccessToken(
                JwtTokenProvider.UserRole.GUARDIAN, bifId, nickname, registrationId, providerUniqueId, socialId
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(providerUniqueId);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(7);
        socialLoginService.saveRefreshToken(socialId, refreshToken, refreshTokenExpiresAt);

        setRefreshTokenCookie(response, refreshToken);

        saveUserSession(request, new CustomUserDetails(accessToken, bifId, nickname, registrationId, providerUniqueId, JwtTokenProvider.UserRole.GUARDIAN, socialId));

        response.sendRedirect(frontendUrl + "/");
    }

    private void handleIncompleteRegistration(SocialLogin socialLogin, String providerUniqueId,
                                              String registrationId,
                                              HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession();
        session.setAttribute("registration_socialId", socialLogin.getSocialId());
        session.setAttribute("registration_email", socialLogin.getEmail());
        session.setAttribute("registration_provider", registrationId);
        session.setAttribute("registration_providerUniqueId", providerUniqueId);


        response.sendRedirect(frontendUrl + "/login/select-role");
    }

    private void handleNewUser(String email, String providerUniqueId,
                               String registrationId,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {

        SocialLogin socialLogin = socialLoginService.createSocialLogin(email,
                SocialLogin.SocialProvider.valueOf(registrationId.toUpperCase()), providerUniqueId);

        HttpSession session = request.getSession();
        session.setAttribute("registration_socialId", socialLogin.getSocialId());
        session.setAttribute("registration_email", email);
        session.setAttribute("registration_provider", registrationId);
        session.setAttribute("registration_providerUniqueId", providerUniqueId);

        response.sendRedirect(frontendUrl + "/login/select-role");
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);
    }

    private void saveUserSession(HttpServletRequest request, CustomUserDetails userDetails) {
        HttpSession session = request.getSession();
        session.setAttribute("accessToken", userDetails.getAccessToken());
        session.setAttribute("providerUniqueId", userDetails.getProviderUniqueId());
        session.setAttribute("userRole", userDetails.getRole().name());
        session.setAttribute("bifId", userDetails.getBifId());
        session.setAttribute("nickname", userDetails.getNickname());
        session.setAttribute("provider", userDetails.getProvider());
        session.setAttribute("socialId", userDetails.getSocialId());
    }

}
