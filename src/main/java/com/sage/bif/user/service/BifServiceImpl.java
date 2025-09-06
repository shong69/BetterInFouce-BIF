package com.sage.bif.user.service;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.common.util.RandomGenerator;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.SocialLogin;
import com.sage.bif.user.event.model.UserWithdrawalEvent;
import com.sage.bif.user.repository.BifRepository;
import com.sage.bif.user.repository.GuardianRepository;
import com.sage.bif.user.repository.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BifServiceImpl implements BifService {

    private final BifRepository bifRepository;
    private final GuardianRepository guardianRepository;
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

        return bifRepository.save(bif);
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

    @Override
    @Transactional
    public void updateNickname(Long socialId, String newNickname) {
        Bif bif = bifRepository.findBySocialLogin_SocialId(socialId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        if (bif.getNickname().equals(newNickname)) {
            return;
        }

        if (bifRepository.findByNickname(newNickname).isPresent()) {
            throw new BaseException(ErrorCode.AUTH_NICKNAME_DUPLICATE);
        }

        if (guardianRepository.findByNickname(newNickname).isPresent()) {
            throw new BaseException(ErrorCode.AUTH_NICKNAME_DUPLICATE);
        }

        bif.setNickname(newNickname);
        bifRepository.save(bif);
    }

    @Transactional
    public void deleteBySocialId(Long socialId) {
        var bifOpt = bifRepository.findBySocialLogin_SocialId(socialId);
        if (bifOpt.isPresent()) {
            Bif bif = bifOpt.get();
            Long bifId = bif.getBifId();

            var guardians = guardianRepository.findByBif_BifId(bifId);

            if(!guardians.isEmpty()) {
                for(var guardian: guardians) {
                    UserWithdrawalEvent guardianEvent = new UserWithdrawalEvent(
                            this,
                            guardian.getSocialLogin().getSocialId(),
                            bifId,
                            JwtTokenProvider.UserRole.GUARDIAN,
                            LocalDateTime.now()
                    );
                    eventPublisher.publishEvent(guardianEvent);
                }
                guardianRepository.deleteAll(guardians);
            }

            bifRepository.delete(bif);

            UserWithdrawalEvent event = new UserWithdrawalEvent(
                    this,
                    socialId,
                    bifId,
                    JwtTokenProvider.UserRole.BIF,
                    LocalDateTime.now()
            );
            eventPublisher.publishEvent(event);
        }
    }

}
