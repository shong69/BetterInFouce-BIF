package com.sage.bif.user.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import lombok.Getter;

@Getter
public class UserEvent extends BaseEvent {

    public UserEvent(Object source) {
        super(source);
    }

    @Override
    public String getEventType() {
        return "USER_REGISTERED";
    }

}
