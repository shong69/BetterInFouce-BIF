package com.sage.bif.user.service;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.common.util.RandomGenerator;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.Guardian;
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
public class GuardianServiceImpl implements GuardianService {

    private final GuardianRepository guardianRepository;
    private final SocialLoginRepository socialLoginRepository;
    private final BifRepository bifRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Guardian registerBySocialId(Long socialId, String email, String connectionCode) {

        SocialLogin socialLogin = socialLoginRepository.findById(socialId)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND));

        Optional<Guardian> existingGuardian = guardianRepository.findBySocialLogin_SocialId(socialId);
        if (existingGuardian.isPresent()) {
            return existingGuardian.get();
        }
        Bif bif = bifRepository.findByConnectionCode(connectionCode)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_INVALID_INVITATION_CODE));

        String nickname = RandomGenerator.generateUniqueNickname("보호자", this::isNicknameExists);

        Guardian guardian = Guardian.builder()
                .socialLogin(socialLogin)
                .bif(bif)
                .nickname(nickname)
                .build();

        return guardianRepository.save(guardian);
    }

    @Override
    public Optional<Guardian> findBySocialId(Long socialId) {
        return guardianRepository.findBySocialLogin_SocialId(socialId);
    }

    private boolean isNicknameExists(String nickname) {
        return guardianRepository.findByNickname(nickname).isPresent();
    }

    @Override
    @Transactional
    public void updateNickname(Long socialId, String newNickname) {
        Guardian guardian = guardianRepository.findBySocialLogin_SocialId(socialId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        if (guardian.getNickname().equals(newNickname)) {
            return;
        }

        if (guardianRepository.findByNickname(newNickname).isPresent()) {
            throw new BaseException(ErrorCode.AUTH_NICKNAME_DUPLICATE);
        }

        if (bifRepository.findByNickname(newNickname).isPresent()) {
            throw new BaseException(ErrorCode.AUTH_NICKNAME_DUPLICATE);
        }

        guardian.setNickname(newNickname);
        guardianRepository.save(guardian);
    }

    @Transactional
    public void deleteBySocialId(Long socialId) {
        var guardianOpt = guardianRepository.findBySocialLogin_SocialId(socialId);
        if (guardianOpt.isPresent()) {
            Guardian guardian = guardianOpt.get();
            Long bifId = guardian.getBif().getBifId();

            guardianRepository.delete(guardian);

            UserWithdrawalEvent event = new UserWithdrawalEvent(
                    this,
                    socialId,
                    bifId,
                    JwtTokenProvider.UserRole.GUARDIAN,
                    LocalDateTime.now()
            );
            eventPublisher.publishEvent(event);
        }
    }
}
