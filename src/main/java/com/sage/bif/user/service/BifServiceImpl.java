package com.sage.bif.user.service;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.util.RandomGenerator;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.event.model.BifRegisteredEvent;
import com.sage.bif.user.event.model.BifRegistrationRequestedEvent;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.user.repository.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BifServiceImpl implements BifService {

    private final BifRepository bifRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Bif registerBySocialId(Long socialId, String email) {

        SocialLogin socialLogin = socialLoginRepository.findById(socialId)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND));

        Optional<Bif> existingBif = bifRepository.findBySocialLogin_SocialId(socialId);
        if (existingBif.isPresent()) {
            return existingBif.get();
        }

        String nickname = RandomGenerator.generateUniqueNickname("사용자", this::isNicknameExists);
        String connectionCode = RandomGenerator.generateUniqueConnectionCode(this::isConnectionCodeExists);

        Bif bif = Bif.builder()
                .socialLogin(socialLogin)
                .nickname(nickname)
                .connectionCode(connectionCode)
                .build();

        BifRegistrationRequestedEvent requestedEvent = new BifRegistrationRequestedEvent(bif);
        eventPublisher.publishEvent(requestedEvent);

        Bif savedBif = bifRepository.save(bif);

        BifRegisteredEvent registeredEvent = new BifRegisteredEvent(savedBif);
        eventPublisher.publishEvent(registeredEvent);

        return savedBif;
    }

    @Override
    public Optional<Bif> findByConnectionCode(String connectionCode) {
        return bifRepository.findByConnectionCode(connectionCode);
    }

    @Override
    public Optional<Bif> findBySocialId(Long socialId) {
        return bifRepository.findBySocialLogin_SocialId(socialId);
    }

    private boolean isNicknameExists(String nickname) {
        return bifRepository.findByNickname(nickname).isPresent();
    }

    private boolean isConnectionCodeExists(String connectionCode) {
        return bifRepository.findByConnectionCode(connectionCode).isPresent();
    }
    
}
