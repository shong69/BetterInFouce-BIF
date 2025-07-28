package com.sage.bif.user.event.listener;

import com.sage.bif.user.event.model.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserRegisteredListener {
    
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("User registered: {} - Method: {} - EventId: {}", 
                event.getUser().getEmail(), event.getRegistrationMethod(), event.getEventId());
        
        // 여기에 사용자 등록 후 처리 로직 추가
        // 예: 환영 이메일 발송, 기본 설정 생성 등
    }
    
    @Async
    @EventListener
    public void handleUserRegisteredAsync(UserRegisteredEvent event) {
        log.info("Async processing user registration: {} - EventId: {}", 
                event.getUser().getEmail(), event.getEventId());
        
        // 비동기 처리 로직
        // 예: 외부 서비스 호출, 알림 발송 등
    }
} 