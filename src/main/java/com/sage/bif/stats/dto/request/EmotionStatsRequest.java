package com.sage.bif.stats.dto.request;

import com.sage.bif.stats.entity.EmotionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionStatsRequest {

    @NotNull(message = "BIF ID는 필수입니다")
    private Long bifId;

    @NotNull(message = "감정은 필수입니다")
    private EmotionType emotion;

    private List<String> keywords;
}
