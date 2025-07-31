package com.sage.bif.stats.dto.request;

import com.sage.bif.stats.entity.EmotionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class StatsCreateRequest {

    @NotNull(message = "BIF ID는 필수입니다")
    private Long bifId;

    @NotNull(message = "연도는 필수입니다")
    @Min(value = 2025, message = "연도는 2025년 이상이어야 합니다")
    private Integer year;

    @NotNull(message = "월은 필수입니다")
    @Min(value = 1, message = "월은 1 이상이어야 합니다")
    @Max(value = 12, message = "월은 12 이하여야 합니다")
    private Integer month;

    private EmotionType emotion;
    private List<String> keywords;
}
