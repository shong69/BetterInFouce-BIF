package com.sage.bif.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialRegistrationRequest {

    @NotNull(message = "소셜 로그인 ID는 필수입니다")
    private Long socialId;

    @NotBlank(message = "이메일은 필수입니다")
    private String email;

    private String connectionCode;

}
