package com.sage.bif.user.event.listener;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.event.model.BifRegistrationRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BifRegistrationRequestedListener {

    @EventListener
    public void handleBifRegistrationRequested(BifRegistrationRequestedEvent event) {
        log.info("BIF 회원가입 요청 처리: {}", event.getBif().getNickname());

        validateBifRegistration(event.getBif());
    }

    private void validateBifRegistration(Bif bif) {
        if (bif.getNickname() == null || bif.getNickname().trim().isEmpty()) {
            throw new BaseException(ErrorCode.AUTH_NICKNAME_INVALID);
        }

        if (bif.getConnectionCode() == null || bif.getConnectionCode().trim().isEmpty()) {
            throw new BaseException(ErrorCode.AUTH_INVALID_INVITATION_CODE);
        }

        log.info("BIF 회원가입 유효성 검증 완료: {}", bif.getNickname());
    }
}
