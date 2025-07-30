package com.sage.bif.common.client.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRequest {
    private String userPrompt; // 사용자 입력만 담당
}
