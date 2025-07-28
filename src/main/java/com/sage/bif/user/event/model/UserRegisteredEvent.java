package com.sage.bif.user.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.user.entity.User;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends BaseEvent {
    
    private final User user;
    private final String registrationMethod;
    
    public UserRegisteredEvent(Object source, User user, String registrationMethod) {
        super(source);
        this.user = user;
        this.registrationMethod = registrationMethod;
    }
    
    public UserRegisteredEvent(Object source, User user, String registrationMethod, String correlationId) {
        super(source, correlationId);
        this.user = user;
        this.registrationMethod = registrationMethod;
    }
    
    @Override
    public String getEventType() {
        return "USER_REGISTERED";
    }
} 