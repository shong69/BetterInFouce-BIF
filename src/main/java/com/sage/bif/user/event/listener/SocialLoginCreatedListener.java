package com.sage.bif.user.event.listener;

import com.sage.bif.user.event.model.SocialLoginCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SocialLoginCreatedListener {

    @EventListener
    public void handleSocialLoginCreated(SocialLoginCreatedEvent event) {
        log.info("소셜 로그인 생성 이벤트 처리: {}", event.getSocialLogin().getEmail());
    }
}
