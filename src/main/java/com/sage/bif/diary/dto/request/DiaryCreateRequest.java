package com.sage.bif.diary.dto.request;

import java.time.LocalDateTime;

import com.sage.bif.diary.model.Emotion;
import com.sage.bif.user.entity.Bif;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaryCreateRequest {
    
    @NotNull(message = "감정은 필수입니다.")
    private Emotion emotion;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;
    
    @NotNull(message = "사용자는 필수입니다.")
    private Bif user;
    
    @NotNull(message = "날짜는 필수입니다.")
    private LocalDateTime date;
}