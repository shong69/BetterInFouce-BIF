package com.sage.bif.user.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.user.entity.Guardian;
import lombok.Getter;

@Getter
public class GuardianRegistrationRequestedEvent extends BaseEvent {

    private final Guardian guardian;

    public GuardianRegistrationRequestedEvent(Guardian guardian) {
        super(guardian);
        this.guardian = guardian;
    }

    @Override
    public String getEventType() {
        return "GUARDIAN_REGISTRATION_REQUESTED";
    }
}
