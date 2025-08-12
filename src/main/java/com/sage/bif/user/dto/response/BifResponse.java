package com.sage.bif.user.dto.response;

import com.sage.bif.user.entity.Bif;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BifResponse {
    private Long bifId;
    private Long socialId;
    private String email;
    private String nickname;
    private String connectionCode;
    private LocalDateTime createdAt;

    public static BifResponse from(Bif bif) {
        return BifResponse.builder()
                .bifId(bif.getBifId())
                .socialId(bif.getSocialLogin().getSocialId())
                .email(bif.getSocialLogin().getEmail())
                .nickname(bif.getNickname())
                .connectionCode(bif.getConnectionCode())
                .createdAt(bif.getCreatedAt())
                .build();
    }

}
