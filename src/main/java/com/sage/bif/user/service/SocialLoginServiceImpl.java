package com.sage.bif.user.service;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.service.RedisService;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.event.model.SocialLoginCreatedEvent;
import com.sage.bif.user.repository.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SocialLoginServiceImpl implements SocialLoginService {

    private final SocialLoginRepository socialLoginRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisService redisService;
    private static final String REDIS_TOKEN = "refresh_token:";

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

        SocialLogin savedSocialLogin = socialLoginRepository.save(socialLogin);

        SocialLoginCreatedEvent event = new SocialLoginCreatedEvent(savedSocialLogin);
        eventPublisher.publishEvent(event);

        return savedSocialLogin;
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
        try {
            String redisKey = REDIS_TOKEN + socialId;
            Duration expiration = Duration.between(LocalDateTime.now(), expiresAt);

            redisService.set(redisKey, refreshToken, expiration);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.COMMON_CACHE_ACCESS_FAILED, e);
        }

    }

    @Override
    public String getRefreshTokenFromRedis(Long socialId) {
        String redisKey = REDIS_TOKEN + socialId;
        Optional<Object> tokenOptional = redisService.get(redisKey);
        if (tokenOptional.isPresent()) {
            return tokenOptional.get().toString();
        }
        return null;
    }

    @Override
    @Transactional
    public void deleteRefreshTokenFromRedis(Long socialId) {
        String redisKey = REDIS_TOKEN + socialId;
        redisService.delete(redisKey);
    }

    @Override
    public boolean validateRefreshToken(Long socialId, String refreshToken) {
        String redisKey = REDIS_TOKEN + socialId;
        Optional<Object> storedTokenOptional = redisService.get(redisKey);

        if (storedTokenOptional.isEmpty()) {
            return false;
        }
        String storedToken = storedTokenOptional.get().toString();
        return storedToken.equals(refreshToken);
    }

}
