package com.sage.bif.diary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaryCreateRequest {
    
    @NotBlank(message = "제목은 필수입니다.")
    private String title;
    
    @NotBlank(message = "내용은 필수입니다.")
    private String content;
    
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;
} 