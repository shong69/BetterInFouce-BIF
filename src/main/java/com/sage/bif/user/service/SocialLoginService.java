package com.sage.bif.user.service;

import com.sage.bif.user.entity.SocialLogin;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SocialLoginService {

    SocialLogin createSocialLogin(String email, SocialLogin.SocialProvider provider, String providerUniqueId);
    Optional<SocialLogin> findBySocialId(Long socialId);
    Optional<SocialLogin> findByProviderUniqueId(String providerUniqueId);
    void saveRefreshToken(Long socialId, String refreshToken, LocalDateTime expiresAt);
    boolean validateRefreshToken(Long socialId, String refreshToken);
    void deleteRefreshTokenFromRedis(Long socialId);
    void deleteBySocialId(Long socialId);

}
