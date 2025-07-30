package com.sage.bif.user.service;

import com.sage.bif.common.util.RandomGenerator;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.event.model.BifRegisteredEvent;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.user.repository.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class BifServiceImpl implements BifService {

    private final BifRepository bifRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Bif registerBySocialId(Long socialId, String email) {
        SocialLogin socialLogin = socialLoginRepository.findById(socialId)
                .orElseThrow(() -> new RuntimeException("Social login not found"));

        Optional<Bif> existingBif = bifRepository.findBySocialLogin_SocialId(socialId);
        if (existingBif.isPresent()) {
            return existingBif.get();
        }

        String nickname = RandomGenerator.generateUniqueNickname(this::isNicknameExists);
        String connectionCode = RandomGenerator.generateUniqueConnectionCode(this::isConnectionCodeExists);

        Bif bif = Bif.builder()
                .socialLogin(socialLogin)
                .nickname(nickname)
                .connectionCode(connectionCode)
                .build();

        Bif savedBif = bifRepository.save(bif);

        BifRegisteredEvent event = new BifRegisteredEvent(savedBif);
        eventPublisher.publishEvent(event);

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