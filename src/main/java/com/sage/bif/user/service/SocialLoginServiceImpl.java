package com.sage.bif.user.service;

import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.event.model.SocialLoginCreatedEvent;
import com.sage.bif.user.repository.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SocialLoginServiceImpl implements SocialLoginService {

    private final SocialLoginRepository socialLoginRepository;
    private final ApplicationEventPublisher eventPublisher;

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
        SocialLogin socialLogin = socialLoginRepository.findById(socialId)
                .orElseThrow(() -> new RuntimeException("Social login not found"));

        socialLogin.setRefreshToken(refreshToken);
        socialLogin.setRefreshTokenExpiresAt(expiresAt);

        socialLoginRepository.save(socialLogin);
    }

    @Override
    public Optional<String> getRefreshToken(Long socialId) {
        return socialLoginRepository.findById(socialId)
                .map(SocialLogin::getRefreshToken);
    }

    @Override
    @Transactional
    public void deleteRefreshToken(Long socialId) {
        SocialLogin socialLogin = socialLoginRepository.findById(socialId)
                .orElseThrow(() -> new RuntimeException("Social login not found"));

        socialLogin.setRefreshToken(null);
        socialLogin.setRefreshTokenExpiresAt(null);

        socialLoginRepository.save(socialLogin);
    }

    @Override
    public boolean validateRefreshToken(Long socialId, String refreshToken) {
        Optional<SocialLogin> socialLoginOpt = socialLoginRepository.findById(socialId);
        if (socialLoginOpt.isEmpty()) {
            return false;
        }

        SocialLogin socialLogin = socialLoginOpt.get();
        String storedRefreshToken = socialLogin.getRefreshToken();
        LocalDateTime expiresAt = socialLogin.getRefreshTokenExpiresAt();

        return storedRefreshToken != null &&
                storedRefreshToken.equals(refreshToken) &&
                expiresAt != null &&
                expiresAt.isAfter(LocalDateTime.now());
    }
}