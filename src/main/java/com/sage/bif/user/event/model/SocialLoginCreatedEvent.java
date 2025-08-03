package com.sage.bif.user.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.user.entity.SocialLogin;
import lombok.Getter;

@Getter
public class SocialLoginCreatedEvent extends BaseEvent {

    private final SocialLogin socialLogin;

    public SocialLoginCreatedEvent(SocialLogin socialLogin) {
        super(socialLogin);
        this.socialLogin = socialLogin;
    }

    @Override
    public String getEventType() {
        return "SOCIAL_LOGIN_CREATED";
    }
}
