package com.sage.bif.diary.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.sage.bif.diary.model.Emotion;

@Getter
@Setter
@Builder
public class DiaryResponse {
    private Long id;
    private Emotion emotion;
    private String content;
    private Long userId;
    private String aiFeedback;
    private boolean contentFlagged;
    private String contentFlaggedCategories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 