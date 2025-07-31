package com.sage.bif.common.oauth;

import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.service.SocialLoginService;
import com.sage.bif.user.service.BifService;
import com.sage.bif.user.service.GuardianService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String PARAM_REFRESH_TOKEN = "&refreshToken=";
    private static final String PARAM_PROVIDER = "&provider=";
    private static final String PARAM_EMAIL = "&email=";
    private static final String PARAM_STATUS = "&status=";

    private final JwtTokenProvider jwtTokenProvider;
    private final SocialLoginService socialLoginService;
    private final BifService bifService;
    private final GuardianService guardianService;

    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider jwtTokenProvider, SocialLoginService socialLoginService,
                                              BifService bifService, GuardianService guardianService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.socialLoginService = socialLoginService;
        this.bifService = bifService;
        this.guardianService = guardianService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = ((OAuth2User) authentication.getPrincipal()).getAttributes();

        OAuth2UserInfo userInfo;

        try {
            userInfo = OAuth2UserInfoFactory.get(registrationId, attributes);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        String email = userInfo.getEmail();
        String providerUniqueId = userInfo.getId();
        SocialLogin.SocialProvider provider = SocialLogin.SocialProvider.valueOf(registrationId.toUpperCase());

        // provider_unique_id로 기존 사용자인지 확인
        var existingSocialLogin = socialLoginService.findByProviderUniqueId(providerUniqueId);

        if (existingSocialLogin.isPresent()) {
            // 기존 사용자: BIF/Guardian 정보 조회하여 JWT 발급
            SocialLogin socialLogin = existingSocialLogin.get();

            // BIF 회원인지 확인
            var bif = bifService.findBySocialId(socialLogin.getSocialId());
            if (bif.isPresent()) {
                // BIF 회원
                String accessToken = jwtTokenProvider.generateAccessToken(
                        email,
                        JwtTokenProvider.UserRole.BIF,
                        bif.get().getBifId(),
                        bif.get().getNickname(),
                        provider.name(),
                        providerUniqueId
                );

                // Refresh Token 생성 및 저장
                String refreshToken = jwtTokenProvider.generateRefreshToken(email);
                LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(30);
                socialLoginService.saveRefreshToken(socialLogin.getSocialId(), refreshToken, refreshTokenExpiresAt);

                String redirectUrl = "/api/token-test.html?accessToken=" + accessToken +
                        PARAM_REFRESH_TOKEN + refreshToken +
                        PARAM_PROVIDER + registrationId +
                        PARAM_EMAIL + email +
                        PARAM_STATUS + "existing";
                response.sendRedirect(redirectUrl);
            } else {
                // Guardian 회원인지 확인
                var guardian = guardianService.findBySocialId(socialLogin.getSocialId());
                if (guardian.isPresent()) {
                    // Guardian 회원
                    String accessToken = jwtTokenProvider.generateAccessToken(
                            email,
                            JwtTokenProvider.UserRole.GUARDIAN,
                            guardian.get().getBif().getBifId(),
                            guardian.get().getNickname(),
                            provider.name(),
                            providerUniqueId
                    );

                    // Refresh Token 생성 및 저장
                    String refreshToken = jwtTokenProvider.generateRefreshToken(email);
                    LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(30);
                    socialLoginService.saveRefreshToken(socialLogin.getSocialId(), refreshToken, refreshTokenExpiresAt);

                    String redirectUrl = "/api/token-test.html?accessToken=" + accessToken +
                            PARAM_REFRESH_TOKEN + refreshToken +
                            PARAM_PROVIDER + registrationId +
                            PARAM_EMAIL + email +
                            PARAM_STATUS + "existing";
                    response.sendRedirect(redirectUrl);
                }
            }
        } else {
            // 신규 사용자: 소셜 로그인 정보 저장 후 회원가입 페이지로
            SocialLogin socialLogin = socialLoginService.createSocialLogin(email, provider, providerUniqueId);
            String redirectUrl = "/api/register-choice.html?socialId=" + socialLogin.getSocialId() +
                    PARAM_EMAIL + email +
                    PARAM_PROVIDER + registrationId;
            response.sendRedirect(redirectUrl);
        }
    }
}