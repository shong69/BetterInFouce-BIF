package com.sage.bif.user.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.user.entity.Bif;
import lombok.Getter;

@Getter
public class BifRegisteredEvent extends BaseEvent {

    private final transient Bif bif;

    public BifRegisteredEvent(Bif bif) {
        super(bif); // Object source를 전달해야 함
        this.bif = bif;
    }

    @Override
    public String getEventType() {
        return "BIF_REGISTERED";
    }
}
