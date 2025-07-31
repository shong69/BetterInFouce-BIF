package com.sage.bif.user.event.listener;

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
            throw new RuntimeException("닉네임이 유효하지 않습니다.");
        }

        if (guardian.getBif() == null) {
            throw new RuntimeException("연결된 BIF 사용자를 찾을 수 없습니다.");
        }

        if (guardian.getSocialLogin() == null) {
            throw new RuntimeException("소셜 로그인 정보를 찾을 수 없습니다.");
        }

        log.info("보호자 회원가입 유효성 검증 완료: {}", guardian.getNickname());
        log.info("연결된 BIF 사용자: {}", guardian.getBif().getNickname());
    }
}