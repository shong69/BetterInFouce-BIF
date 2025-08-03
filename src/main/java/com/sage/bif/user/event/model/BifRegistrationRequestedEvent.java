package com.sage.bif.user.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.user.entity.Bif;
import lombok.Getter;

@Getter
public class BifRegistrationRequestedEvent extends BaseEvent {

    private final Bif bif;

    public BifRegistrationRequestedEvent(Bif bif) {
        super(bif);
        this.bif = bif;
    }

    @Override
    public String getEventType() {
        return "BIF_REGISTRATION_REQUESTED";
    }
}
