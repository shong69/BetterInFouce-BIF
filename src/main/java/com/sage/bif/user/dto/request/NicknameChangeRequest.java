package com.sage.bif.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NicknameChangeRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 20, message = "닉네임은 20자 이내여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다")
    private String nickname;

}
