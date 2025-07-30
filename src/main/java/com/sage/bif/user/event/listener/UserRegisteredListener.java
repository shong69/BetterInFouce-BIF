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
    }
    
    @Async
    @EventListener
    public void handleUserRegisteredAsync(UserRegisteredEvent event) {
        log.info("Async processing user registration: {} - EventId: {}", 
                event.getUser().getEmail(), event.getEventId());
    }
} 