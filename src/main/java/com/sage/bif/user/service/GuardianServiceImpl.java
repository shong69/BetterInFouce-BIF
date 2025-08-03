package com.sage.bif.user.service;

import com.sage.bif.common.util.RandomGenerator;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.Guardian;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.event.model.GuardianRegisteredEvent;
import com.sage.bif.user.event.model.GuardianRegistrationRequestedEvent;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.user.repository.GuardianRepository;
import com.sage.bif.user.repository.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuardianServiceImpl implements GuardianService {

    private final GuardianRepository guardianRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final BifRepository bifRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Guardian registerBySocialId(Long socialId, String email, String connectionCode) {
        SocialLogin socialLogin = socialLoginRepository.findById(socialId)
                .orElseThrow(() -> new RuntimeException("Social login not found"));

        Optional<Guardian> existingGuardian = guardianRepository.findBySocialLogin_SocialId(socialId);
        if (existingGuardian.isPresent()) {
            return existingGuardian.get();
        }

        Bif bif = bifRepository.findByConnectionCode(connectionCode)
                .orElseThrow(() -> new RuntimeException("Invalid connection code"));

        String nickname = RandomGenerator.generateUniqueNickname(this::isNicknameExists);

        Guardian guardian = Guardian.builder()
                .socialLogin(socialLogin)
                .bif(bif)
                .nickname(nickname)
                .build();

        GuardianRegistrationRequestedEvent requestedEvent = new GuardianRegistrationRequestedEvent(guardian);
        eventPublisher.publishEvent(requestedEvent);

        Guardian savedGuardian = guardianRepository.save(guardian);

        GuardianRegisteredEvent registeredEvent = new GuardianRegisteredEvent(savedGuardian);
        eventPublisher.publishEvent(registeredEvent);


        return savedGuardian;
    }

    @Override
    public Optional<Guardian> findBySocialId(Long socialId) {
        return guardianRepository.findBySocialLogin_SocialId(socialId);
    }

    private boolean isNicknameExists(String nickname) {
        return guardianRepository.findByNickname(nickname).isPresent();
    }
}
