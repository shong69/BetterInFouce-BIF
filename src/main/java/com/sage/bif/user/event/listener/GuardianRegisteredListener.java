package com.sage.bif.user.event.listener;

import com.sage.bif.user.event.model.GuardianRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@SuppressWarnings("unused")
public class GuardianRegisteredListener {

    @EventListener
    public void handleGuardianRegistered(GuardianRegisteredEvent event) {

        log.info("보호자 회원가입 완료 처리: {}", event.getGuardian().getNickname());
        log.info("보호자 회원가입이 성공적으로 완료되었습니다: {}", event.getGuardian().getNickname());
    }
}