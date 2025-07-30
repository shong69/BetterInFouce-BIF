package com.sage.bif.diary.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DiaryResponse {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String aiFeedback;
    private String emotionAnalysis;
    private String summary;
    private LocalDateTime createdAt;
} 