package com.sage.bif.diary.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sage.bif.diary.model.Emotion;

@Getter
@Setter
@Builder
public class DiaryResponse {

    private UUID id;
    private Emotion emotion;
    private String content;
    private Long userId;
    private String aiFeedback;
    private boolean contentFlagged;
    private String contentFlaggedCategories;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
