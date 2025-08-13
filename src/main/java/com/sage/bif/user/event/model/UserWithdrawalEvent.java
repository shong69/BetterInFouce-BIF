package com.sage.bif.user.event.model;

import com.sage.bif.common.event.model.BaseEvent;
import com.sage.bif.common.jwt.JwtTokenProvider;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserWithdrawalEvent extends BaseEvent {

    private final Long socialId;
    private final Long bifId;
    private final JwtTokenProvider.UserRole userRole;
    private final LocalDateTime withdrawalDate;

    public UserWithdrawalEvent(Object source, Long socialId, Long bifId,
                               JwtTokenProvider.UserRole userRole, LocalDateTime withdrawalDate) {
        super(source);
        this.socialId = socialId;
        this.bifId = bifId;
        this.userRole = userRole;
        this.withdrawalDate = withdrawalDate;
    }

    @Override
    public String getEventType() {
        return "USER_WITHDRAWAL";
    }

}
