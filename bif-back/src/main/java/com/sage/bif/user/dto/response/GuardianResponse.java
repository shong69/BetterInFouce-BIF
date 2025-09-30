package com.sage.bif.user.dto.response;

import com.sage.bif.user.entity.Guardian;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GuardianResponse {
    private Long guardianId;
    private Long socialId;
    private Long bifId;
    private String email;
    private String nickname;
    private LocalDateTime createdAt;

    public static GuardianResponse from(Guardian guardian) {
        return GuardianResponse.builder()
                .guardianId(guardian.getGuardianId())
                .socialId(guardian.getSocialLogin().getSocialId())
                .bifId(guardian.getBif().getBifId())
                .email(guardian.getSocialLogin().getEmail())
                .nickname(guardian.getNickname())
                .createdAt(guardian.getCreatedAt())
                .build();
    }

}
