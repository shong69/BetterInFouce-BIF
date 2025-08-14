package com.sage.bif.diary.dto.request;

import com.sage.bif.diary.model.Emotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryRequest {

    @NotNull(message = "감정은 필수입니다.")
    private Emotion emotion;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;
}
