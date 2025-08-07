package com.sage.bif.user.event.listener;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.user.entity.Guardian;
import com.sage.bif.user.event.model.GuardianRegistrationRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@SuppressWarnings("unused")
public class GuardianRegistrationRequestedListener {

    @EventListener
    public void handleGuardianRegistrationRequested(GuardianRegistrationRequestedEvent event) {
        log.info("보호자 회원가입 요청 처리: {}", event.getGuardian().getNickname());

        validateGuardianRegistration(event.getGuardian());
    }

    private void validateGuardianRegistration(Guardian guardian) {
        if (guardian.getNickname() == null || guardian.getNickname().trim().isEmpty()) {
            throw new BaseException(ErrorCode.AUTH_NICKNAME_INVALID);
        }

        if (guardian.getBif() == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }

        if (guardian.getSocialLogin() == null) {
            throw new BaseException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }

        log.info("보호자 회원가입 유효성 검증 완료: {}", guardian.getNickname());
        log.info("연결된 BIF 사용자: {}", guardian.getBif().getNickname());
    }
}
