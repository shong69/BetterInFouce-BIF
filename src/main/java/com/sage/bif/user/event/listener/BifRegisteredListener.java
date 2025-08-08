package com.sage.bif.user.event.listener;

import com.sage.bif.user.event.model.BifRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@SuppressWarnings("unused")
public class BifRegisteredListener {

    @EventListener
    public void handleBifRegistered(BifRegisteredEvent event) {

        log.info("BIF 회원가입 완료 처리: {}", event.getBif().getNickname());
        log.info("BIF 회원가입이 성공적으로 완료되었습니다: {}", event.getBif().getNickname());
    }
}
