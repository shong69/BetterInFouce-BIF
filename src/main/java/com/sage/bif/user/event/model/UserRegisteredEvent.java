package com.sage.bif.user.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.user.entity.Bif;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends BaseEvent {
    
    private final Bif user;
    private final String registrationMethod;
    
    public UserRegisteredEvent(Object source, Bif user, String registrationMethod) {
        super(source);
        this.user = user;
        this.registrationMethod = registrationMethod;
    }
    
    public UserRegisteredEvent(Object source, Bif user, String registrationMethod, String correlationId) {
        super(source, correlationId);
        this.user = user;
        this.registrationMethod = registrationMethod;
    }
    
    @Override
    public String getEventType() {
        return "USER_REGISTERED";
    }
} 