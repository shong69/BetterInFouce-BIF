package com.sage.bif.user.service;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.repository.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
//@RequiredArgsConstructor
public class SocialLoginServiceImpl implements SocialLoginService {

    private final SocialLoginRepository socialLoginRepository;
//    private final RedisTemplate<String, String> redisTemplate;

//    private static final String REDIS_TOKEN = "refresh_token:";
//    private final ConcurrentHashMap<Long, String> refreshTokenStore = new ConcurrentHashMap<>();

    private final JwtTokenProvider jwtTokenProvider;

    public SocialLoginServiceImpl(SocialLoginRepository socialLoginRepository,
                                  JwtTokenProvider jwtTokenProvider) {
        this.socialLoginRepository = socialLoginRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public SocialLogin createSocialLogin(String email, SocialLogin.SocialProvider provider, String providerUniqueId) {

        Optional<SocialLogin> existingSocialLogin = socialLoginRepository.findByProviderUniqueId(providerUniqueId);
        if (existingSocialLogin.isPresent()) {
            return existingSocialLogin.get();
        }

        SocialLogin socialLogin = SocialLogin.builder()
                .email(email)
                .provider(provider)
                .providerUniqueId(providerUniqueId)
                .build();

        return socialLoginRepository.save(socialLogin);
    }

    @Override
    public Optional<SocialLogin> findBySocialId(Long socialId) {
        return socialLoginRepository.findById(socialId);
    }

    @Override
    public Optional<SocialLogin> findByProviderUniqueId(String providerUniqueId) {

        return socialLoginRepository.findByProviderUniqueId(providerUniqueId);
    }

    @Override
    @Transactional
    public void saveRefreshToken(Long socialId, String refreshToken, LocalDateTime expiresAt) {

//        try {
//            String redisKey = REDIS_TOKEN + socialId;
//            Duration expiration = Duration.between(LocalDateTime.now(), expiresAt);
//
//            redisTemplate.opsForValue().set(redisKey, refreshToken, expiration);
//        } catch (Exception e) {
//            throw new BaseException(ErrorCode.COMMON_CACHE_ACCESS_FAILED, e);
//        }
//        refreshTokenStore.put(socialId, refreshToken);
        log.debug("Using stateless JWT - no server-side refresh token storage");
    }

    @Override
    @Transactional
    public void deleteRefreshTokenFromRedis(Long socialId) {
//
//        String redisKey = REDIS_TOKEN + socialId;
//        redisTemplate.delete(redisKey);
//        refreshTokenStore.remove(socialId);
        log.debug("Using stateless JWT - no server-side refresh token deletion needed");
    }

    @Override
    public boolean validateRefreshToken(Long socialId, String refreshToken) {
//
//        String redisKey = REDIS_TOKEN + socialId;
//        String storedToken = redisTemplate.opsForValue().get(redisKey);
//
//        if (storedToken == null) {
//            return false;
//        }
//        return storedToken.equals(refreshToken);

//        String storedToken = refreshTokenStore.get(socialId);
//        return storedToken != null && storedToken.equals(refreshToken);

        return jwtTokenProvider.isRefreshToken(refreshToken) &&
                "SUCCESS".equals(jwtTokenProvider.validateToken(refreshToken));
    }

    @Transactional
    public void deleteBySocialId(Long socialId) {

        socialLoginRepository.deleteById(socialId);
    }

}
